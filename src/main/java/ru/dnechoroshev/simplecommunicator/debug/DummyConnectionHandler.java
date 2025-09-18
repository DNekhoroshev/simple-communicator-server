package ru.dnechoroshev.simplecommunicator.debug;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import ru.dnechoroshev.simplecommunicator.model.ConnectionDto;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

import static ru.dnechoroshev.simplecommunicator.util.ConnectionState.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DummyConnectionHandler {

    //private final ExecutorService techPool =  Executors.newSingleThreadExecutor();

    private static final String SERVER_ADDRESS = "127.0.0.1";

    @Getter
    private boolean alive;

    public void handleConnection(@NonNull ConnectionDto connection) {
        log.info("Соединяемся с {}:{}", SERVER_ADDRESS, connection.port());
        final ExecutorService executor = Executors.newFixedThreadPool(5);
        executor.submit(() -> {
            try (Socket socket = new Socket(SERVER_ADDRESS, connection.port())) {
                log.info("Соединение установлено");
                alive = true;
                final InputStream inputStream = socket.getInputStream();
                final OutputStream outputStream = socket.getOutputStream();

                DataInputStream dis = new DataInputStream(inputStream);

                int status = WAITING_FOR_RESPONSE.getState();
                while (status == WAITING_FOR_RESPONSE.getState()) {
                    log.info("Ожидаем ответа сервера...");
                    status = dis.readInt();
                }

                processStatus(status);

                log.info("Начало разговора");

                DummyInputReader reader = new DummyInputReader(inputStream);
                Future<File> grabbedAudioFile = executor.submit(reader);

                CompletableFuture<Void> f1 = CompletableFuture
                        .supplyAsync(() -> {
                            DummyTestAudioSender.sendAudioFileToOut("static/recorded_audio.wav", outputStream);
                            return null;
                        }, executor)
                        .thenApplyAsync(result -> {
                            reader.startAudioGrabbingFor(10000);
                            return getFile(grabbedAudioFile);
                        }).thenAccept(result -> {
                            try (FileInputStream fis = new FileInputStream(result)) {
                                DummyTestAudioSender.sendBytesToOut(fis.readAllBytes(), outputStream);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

                f1.join();
                log.info("Разговор завершен");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                alive = false;
                executor.shutdown();
            }
        });
    }

    private static File getFile(Future<File> grabbedAudioFile) {
        try {
            return grabbedAudioFile.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void processStatus(int status) {
        if (status == CONNECTION_STARTING.getState()) {
            log.info("Ответ сервера получен");
            if (!alive) {
                throw new RuntimeException("Невозможно установить соединение");
            }
        } else if (status == CONNECTION_TIMEOUT.getState()) {
            log.warn("Таймаут соединения с сервером");
            alive = false;
        } else {
            log.error("Недопустимый код ответа: {}", status);
            alive = false;
            throw new RuntimeException("Недопустимый код ответа");
        }
    }

    public void hangUp() {
        log.info("Вешаем трубку...");
        alive = false;
    }

}
