package ru.dnechoroshev.simplecommunicator.model;

import lombok.extern.slf4j.Slf4j;
import ru.dnechoroshev.simplecommunicator.exception.CommunicationException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;

@Slf4j
public class Callee extends AbstractParticipant {

    public Callee(String name, int port, AbstractParticipant correspondent) {
        super(name, port, correspondent);
    }

    public Callee(String name, int port) {
        super(name, port);
    }

    @Override
    public void connect() {
        Thread connectionListener = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log.info("Слушаем порт {}", port);
                socket = serverSocket.accept();
                log.info("Подключен клиент: {}", socket.getInetAddress());
                connected = true;

                var out = new DataOutputStream(getOutputStream());
                var remainingTime = TIMEOUT_SECONDS;

                while (connected && remainingTime > 0 && !correspondent.ready) {
                    log.info("Ожидаем готовности вызывающего...");
                    log.info("{} ожидает ответа от {}", this.name, correspondent.getName());
                    out.writeInt(WAITING_FOR_RESPONSE);
                    Thread.sleep(CHECK_DELAY_MS);
                    remainingTime = remainingTime - CHECK_DELAY_MS;
                    log.info("Оставшееся время: {} мс", remainingTime);
                }
                if (remainingTime > 0) {
                    log.info("{} : получен ответ от {}", name, correspondent.getName());
                    out.writeInt(CONNECTION_STARTING);
                    ready = true;
                    try (var in = new BufferedInputStream(getInputStream()); var calleeOut = new BufferedOutputStream(correspondent.getOutputStream());) {
                        in.transferTo(calleeOut);
                        log.info("{}: Звонок завершен", name);
                    } catch (IOException e) {
                        processError(e);
                    }
                } else {
                    log.info("Таймаут соединения {} с {}", name, correspondent.getName());
                    out.writeInt(CONNECTION_TIMEOUT);
                }
            } catch (IOException | InterruptedException e) {
                log.error("Ошибка коммуникации", e);
                throw new CommunicationException("Ошибка коммуникации", e);
            } finally {
                close();
            }
        }
        );

        connectionListener.start();
    }
}
