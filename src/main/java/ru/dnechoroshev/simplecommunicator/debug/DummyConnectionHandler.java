package ru.dnechoroshev.simplecommunicator.debug;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import ru.dnechoroshev.simplecommunicator.model.ConnectionDto;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DummyConnectionHandler {

    private final ExecutorService techPool =  Executors.newSingleThreadExecutor();

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int WAITING_FOR_RESPONSE = 0xFFAA001;
    private static final int CONNECTION_STARTING = 0xFFAA002;
    private static final int CONNECTION_TIMEOUT = 0xFFAA003;

    @Getter
    private boolean alive;

    public void handleConnection(@NonNull ConnectionDto connection) {
        log.info("Соединяемся с {}:{}", SERVER_ADDRESS, connection.port());
        techPool.submit(() -> {
            try (Socket socket = new Socket(SERVER_ADDRESS, connection.port())) {
                log.info("Соединение установлено");
                alive = true;
                final InputStream inputStream = socket.getInputStream();
                final OutputStream outputStream = socket.getOutputStream();

                DataInputStream dis = new DataInputStream(inputStream);
                int status = WAITING_FOR_RESPONSE;
                while (status == WAITING_FOR_RESPONSE) {
                    log.info("Ожидаем ответа сервера...");
                    status = dis.readInt();
                }

                if (status == CONNECTION_STARTING) {
                    log.info("Ответ сервера получен");
                } else if (status == CONNECTION_TIMEOUT) {
                    log.warn("Таймаут соединения с сервером");
                    alive = false;
                    return;
                } else {
                    log.error("Недопустимый код ответа: {}", status);
                    alive = false;
                    throw new RuntimeException("Недопустимый код ответа");
                }
                log.info("Начало разговора");

                final ExecutorService executor = Executors.newFixedThreadPool(2);

                DummyInputReader reader = new DummyInputReader(inputStream);

                Future<File> grabbedAudioFile = executor.submit(reader);

                CompletableFuture f1 = CompletableFuture.supplyAsync(() -> {
                    ClassPathResource resource = new ClassPathResource("static/recorded_audio.wav");
                    try (InputStream audioIn = resource.getInputStream()) {
                        log.info("Передача тестового сообщения");
                        audioIn.transferTo(outputStream);
                        Thread.sleep(5000);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                }, executor);

                CompletableFuture<File> f2 = f1.thenApply( result -> {
                    reader.startAudioGrabbinfFor(10000);
                    try {
                        return grabbedAudioFile.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                CompletableFuture<Void> f3 = f2.thenAccept(result -> {
                    try(FileInputStream fis = new FileInputStream(result)) {
                        byte[] message = fis.readAllBytes();
                        log.info("Возврат тестового сообщения");
                        outputStream.write(message);
                        Thread.sleep(10000);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
                f3.join();
                log.info("Разговор завершен");
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                alive = false;
            }
        });
    }

    public void hangUp() {
        log.info("Вешаем трубку...");
        alive = false;
    }

}
