package ru.dnechoroshev.simplecommunicator.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.dnechoroshev.simplecommunicator.server.CommunicationDispatcher;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class SessionChecker {

    private static final int CALL_TIMEOUT_MS = 20000;

    private final CommunicationDispatcher dispatcher;

    @Scheduled(fixedRate = 5000)
    public void checkSession() {
        dispatcher
                .getCalls()
                .filter(session -> session.getCreationTime().plusMillis(CALL_TIMEOUT_MS).isBefore(Instant.now()))
                .forEach(session -> {
                    log.info("Таймаут сессии {}", session);
                    dispatcher.closeSession(session);
                });
    }
}
