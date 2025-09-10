package ru.dnechoroshev.simplecommunicator.server;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.dnechoroshev.simplecommunicator.model.Participant;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommunicationDispatcher {

    private final ConnectionFactory connectionFactory;

    private final Map<String, Participant> actualCalls = new ConcurrentHashMap<>();

    private final Map<String, Participant> liveCommunications = new ConcurrentHashMap<>();

    public String checkIncomingCall(String calleeName) {
        log.info("Actual calls exist for {}", actualCalls.keySet());
        return actualCalls.containsKey(calleeName)
                ? actualCalls.get(calleeName).getName()
                : null;
    }

    @SneakyThrows
    public int startCall(String callerName, String calleeName) {
        Participant caller = connectionFactory.createConnectedParticipant(callerName, calleeName);
        actualCalls.put(calleeName, caller);
        liveCommunications.put(callerName, caller);
        return caller.getPort();
    }

    public void endCall(String callerName, String calleeName) {

        String targetPartName = null;
        if (StringUtils.isNotBlank(calleeName)) {
            targetPartName = calleeName;
        } else if (StringUtils.isNotBlank(callerName)) {
            targetPartName = callerName;
        }

        if (targetPartName != null) {
            Participant participant = liveCommunications.get(targetPartName);
            log.info("Закрываем соединение {} -> {}", participant.getName(), participant.getCorrespondent().getName() );
            participant.close();
            participant.getCorrespondent().close();
            liveCommunications.remove(participant.getName());
            liveCommunications.remove(participant.getCorrespondent().getName());
            connectionFactory.releasePort(
                    participant.getPort(),
                    participant.getCorrespondent().getPort()
            );
        }

    }

    public int acceptCall(String calleeName) {
        if (actualCalls.containsKey(calleeName)) {
            try {
                Participant callee = actualCalls.get(calleeName).getCorrespondent();
                callee.connect();
                liveCommunications.put(calleeName, callee);
                return callee.getPort();
            } catch (Exception e) {
                log.error("Ошибка установления соединения", e);
                throw new RuntimeException(e);
            } finally {
                actualCalls.remove(calleeName);
            }
        } else {
            log.error("Не найден активный звонок для {}", calleeName);
            throw new RuntimeException("Не найден активный звонок");
        }
    }

    public void stopAllCommunications() {
        liveCommunications.forEach((k ,v) -> {
            log.info("Закрываем соединение для {}", k);
            v.close();
        });
    }

}
