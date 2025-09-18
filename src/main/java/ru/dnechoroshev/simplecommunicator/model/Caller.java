package ru.dnechoroshev.simplecommunicator.model;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;

@Slf4j
public class Caller extends AbstractParticipant {

    public Caller(String name, int port, AbstractParticipant correspondent) {
        super(name, port, correspondent);
    }

    public Caller(String name, int port) {
        super(name, port);
    }

    @Override
    public void connect() {
        Thread connectionListener = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log.info("Слушаем порт {}", port);
                socket = serverSocket.accept();
                connected = true;
                log.info("Подключен клиент: {}", socket.getInetAddress());

                var out = new DataOutputStream(getOutputStream());
                var remainingTime = TIMEOUT_SECONDS;

                while (connected && remainingTime > 0 && !correspondent.connected) {
                    log.info("Ожидаем ответа...");
                    log.info("{} ожидает ответа от {}", this.name, correspondent.getName());
                    out.writeInt(WAITING_FOR_RESPONSE);
                    Thread.sleep(CHECK_DELAY_MS);
                    remainingTime = remainingTime - CHECK_DELAY_MS;
                    log.info("Оставшееся время: {} мс", remainingTime);
                }
                if (connected && remainingTime > 0) {
                    log.info("{} : получен ответ от {}", name, correspondent.getName());
                    out.writeInt(CONNECTION_STARTING);
                    ready = true;
                    while (!correspondent.ready) {
                        Thread.sleep(CHECK_DELAY_MS);
                    }
                    try (var in = new BufferedInputStream(getInputStream()); var calleeOut = new BufferedOutputStream(correspondent.getOutputStream());) {
                        in.transferTo(calleeOut);
                    } catch (IOException e) {
                        log.debug("Звонок завершен. Детальное сообщение: {}", e.getMessage());
                    } finally {
                        log.info("{}: Звонок завершен", name);
                    }
                } else if (connected) {
                    log.info("Таймаут соединения {} с {}", name, correspondent.getName());
                    out.writeInt(CONNECTION_TIMEOUT);
                } else {
                    log.info("Соединение прервано");
                }
            } catch (IOException | InterruptedException e) {
                log.error("Ошибка коммуникации", e);
                throw new RuntimeException(e);
            } finally {
                close();
            }
        }
        );

        connectionListener.start();
    }
}
