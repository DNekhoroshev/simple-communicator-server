package ru.dnechoroshev.simplecommunicator.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.dnechoroshev.simplecommunicator.model.AbstractParticipant;

import java.io.DataOutputStream;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class CallerHandler implements Runnable {

    private static final int WAITING_FOR_RESPONSE = 0xFFAA001;
    private static final int CONNECTION_STARTING = 0xFFAA002;
    private static final int CONNECTION_TIMEOUT = 0xFFAA003;

    private static final int TIMEOUT_SECONDS = 15*1000;
    private static final int CHECK_DELAY_MS = 200;

    private final AbstractParticipant participant;

    @Getter
    @Setter
    private boolean waitingForResponse = true;

    @Getter
    @Setter
    private boolean connectionAlive = true;

    @Override
    public void run() {
        log.info("Participant {} waiting for connection", participant.getName());
        participant.connect();
        log.info("Participant {} connected", participant.getName());
        try {
            // Мы его тут специально не закрываем, т.к. основной обмен пойдет в другом потоке, тут только handshake
            var callerOut = new DataOutputStream(participant.getOutputStream());
            var remainingTime = TIMEOUT_SECONDS;
            while (participant.getCorrespondent().getOutputStream() == null && remainingTime > 0) {
                log.info("{} ожидает ответа от {}", participant.getName(), participant.getCorrespondent().getName());
                callerOut.writeInt(WAITING_FOR_RESPONSE);
                log.info("Remaining time {}", remainingTime);
                Thread.sleep(CHECK_DELAY_MS);
                remainingTime = remainingTime - CHECK_DELAY_MS;
                log.info("Remaining time {}", remainingTime);
            }

            if (remainingTime > 0) {
                log.info("{} соединился с {}", participant.getName(), participant.getCorrespondent().getName());
                callerOut.writeInt(CONNECTION_STARTING);
            } else {
                log.info("Таймаут соединения {} с {}", participant.getName(), participant.getCorrespondent().getName());
                callerOut.writeInt(CONNECTION_TIMEOUT);
                callerOut.close();
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
