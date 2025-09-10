package ru.dnechoroshev.simplecommunicator.server;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import ru.dnechoroshev.simplecommunicator.model.Callee;
import ru.dnechoroshev.simplecommunicator.model.Caller;
import ru.dnechoroshev.simplecommunicator.model.Participant;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

@Service
public class ConnectionFactory {

    private static final int INITIAL_COMM_PORT = 21000;
    private static final int MAX_OPEN_PORTS = 100;

    private final Set<Integer> portBag = Collections.synchronizedSortedSet(new TreeSet<>());

    @PostConstruct
    public void init() {
        for (int i = 0; i < MAX_OPEN_PORTS; i++) {
            portBag.add(INITIAL_COMM_PORT + i);
        }
    }

    public Participant createConnectedParticipant(String callerName, String calleeName) {
        Participant p = new Caller(callerName, this.getFreePort(), new Callee(calleeName, this.getFreePort()));
        p.connect();
        return p;
    }

    public void releasePort(int... portList) {
        for (int port : portList) {
            portBag.add(port);
        }
    }

    private synchronized int getFreePort() {
        int freePort = portBag.stream().mapToInt(Integer::intValue).min().orElseThrow();
        portBag.remove(freePort);
        return freePort;
    }

}
