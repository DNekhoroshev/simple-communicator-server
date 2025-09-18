package ru.dnechoroshev.simplecommunicator.server;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.dnechoroshev.simplecommunicator.exception.ConnectionHandshakeException;
import ru.dnechoroshev.simplecommunicator.model.CallSession;
import ru.dnechoroshev.simplecommunicator.model.AbstractParticipant;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommunicationDispatcher {

    private final ConnectionFactory connectionFactory;

    private final Map<String, CallSession> actualCalls = new ConcurrentHashMap<>();

    private final Map<String, CallSession> liveCommunications = new ConcurrentHashMap<>();

    public String checkIncomingCall(String calleeName) {
        log.info("Actual calls exist for {}", actualCalls.keySet());
        return actualCalls.containsKey(calleeName)
                ? actualCalls.get(calleeName).getCaller().getName()
                : null;
    }

    public Stream<CallSession> getCalls() {
        return actualCalls.values().stream();
    }

    @SneakyThrows
    public int startCall(String callerName, String calleeName) {
        AbstractParticipant caller = connectionFactory.createConnectedParticipant(callerName, calleeName);
        CallSession session = new CallSession(caller, caller.getCorrespondent());
        actualCalls.put(calleeName, session);
        liveCommunications.put(callerName, session);
        return caller.getPort();
    }

    public void endCall(String participantName) {
        log.info("{} вешает трубку", participantName);
        CallSession session = actualCalls.containsKey(participantName)
                ? actualCalls.get(participantName)
                : liveCommunications.get(participantName);
        closeSession(session);

    }

    public void closeSession(CallSession session) {
        if (session != null) {
            AbstractParticipant caller = session.getCaller();
            AbstractParticipant callee = session.getCallee();
            log.info("Закрываем соединение {} -> {}", caller, callee);
            session.close();
            liveCommunications.remove(caller.getName());
            liveCommunications.remove(callee.getName());
            actualCalls.remove(callee.getName());
            connectionFactory.releasePorts(session);
        }
    }

    public int acceptCall(String calleeName) {
        if (actualCalls.containsKey(calleeName)) {
            try {
                CallSession session = actualCalls.get(calleeName);
                AbstractParticipant callee = session.getCallee();
                callee.connect();
                liveCommunications.put(calleeName, session);
                return callee.getPort();
            } catch (Exception e) {
                log.error("Ошибка установления соединения", e);
                throw new ConnectionHandshakeException("Ошибка установления соединения", e);
            } finally {
                actualCalls.remove(calleeName);
            }
        } else {
            log.error("Не найден активный звонок для {}", calleeName);
            throw new ConnectionHandshakeException(calleeName + ": не найден активный звонок");
        }
    }

    public void stopAllCommunications() {
        liveCommunications.forEach((k ,v) -> {
            log.info("Закрываем соединение для {}", k);
            v.close();
        });
    }

}
