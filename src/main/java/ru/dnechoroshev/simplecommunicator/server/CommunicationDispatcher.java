package ru.dnechoroshev.simplecommunicator.server;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.dnechoroshev.simplecommunicator.model.Participant;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CommunicationDispatcher {

    private static final int INITIAL_COMM_PORT = 21000;
    private static final int MAX_OPEN_PORTS = 100;

    private final Set<Integer> portBag = Collections.synchronizedSortedSet(new TreeSet<>());
    private final Map<String, Participant> actualCalls = new ConcurrentHashMap<>();
    private final Set<Communication> liveCommunications = Collections.synchronizedSet(new HashSet<>());

    @PostConstruct
    public void init() {
        for (int i = 0; i < MAX_OPEN_PORTS; i++) {
            portBag.add(INITIAL_COMM_PORT + i);
        }
    }

    public String checkIncomingCall(String calleeName) {
        return actualCalls.containsKey(calleeName)
                ? actualCalls.get(calleeName).getName()
                : null;
    }

    public int startCall(String callerName, String calleeName) {
        Participant caller = new Participant(callerName, this.getFreePort(), new Participant(calleeName, this.getFreePort()));
        actualCalls.put(calleeName, caller);
        Communication communication = new Communication(caller, caller.getCorrespondent());
        communication.touchCaller();
        liveCommunications.add(communication);
        return caller.getPort();
    }

    public void endCall(String callerName, String calleeName) {
        Communication targetComm = null;
        if (StringUtils.isNotBlank(calleeName)) {
            targetComm = liveCommunications.stream()
                    .filter(c -> c.getCallee().getName().equals(calleeName))
                    .findAny().orElse(null);
        } else if (StringUtils.isNotBlank(callerName)) {
            targetComm = liveCommunications.stream()
                    .filter(c -> c.getCaller().getName().equals(callerName))
                    .findAny().orElse(null);
        }

        if (targetComm != null) {
            log.info("Закрываем соединение {} -> {}", targetComm.getCaller().getName(), targetComm.getCallee().getName() );
            targetComm.close();
            liveCommunications.remove(targetComm);
        }

    }

    public int acceptCall(String calleeName) {
        if (actualCalls.containsKey(calleeName)) {
            try {
                Participant callee = actualCalls.get(calleeName).getCorrespondent();
                Communication currentCommmunication = liveCommunications
                        .stream()
                        .filter(c -> c.getCallee().getName().equals(calleeName))
                        .findAny()
                        .orElseThrow();

                currentCommmunication.touchCallee();
                currentCommmunication.connectParticipants();
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

    public void stopCommunicationByCaller(String callerName) {
        liveCommunications.stream()
                .filter(comm -> comm.getCaller().getName().equals(callerName))
                .forEach(c -> c.close());
    }

    public void stopCommunicationByCallee(String calleeName) {
        liveCommunications.stream()
                .filter(comm -> comm.getCallee().getName().equals(calleeName))
                .forEach(c -> c.close());
    }

    public void stopAllCommunications() {
        liveCommunications.stream().forEach(c -> c.close());
    }

    private synchronized int getFreePort() {
        int freePort = portBag.stream().mapToInt(Integer::intValue).min().orElseThrow();
        portBag.remove(freePort);
        return freePort;
    }

}
