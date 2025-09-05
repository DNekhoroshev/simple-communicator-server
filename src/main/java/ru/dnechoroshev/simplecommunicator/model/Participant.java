package ru.dnechoroshev.simplecommunicator.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

@Data
@Slf4j
public class Participant implements Closeable {

    // Имя абонента
    private String name;

    // Вызываемый/вызывающий абонент
    private Participant correspondent;

    // Выделяемый системой порт для текущего абонента
    private int port;

    private Socket socket;

    private Thread communicationThread;

    private volatile boolean alive = true;

    public Participant(String name, int port, Participant correspondent) {
        this.name = name;
        this.port = port;
        this.correspondent = correspondent;
        if (correspondent != null) {
            correspondent.setCorrespondent(this);
        }
    }

    public Participant(String name, int port) {
        this(name, port, null);
    }

    public void connect() {
        communicationThread = new Thread(() -> {
            try (var s = new ServerSocket(port)) {
                log.debug("Слушаем порт {}", port);
                try (Socket incomingSocket = s.accept()) {
                    log.info("Подключен клиент: {}", incomingSocket.getInetAddress());
                    socket = incomingSocket;
                    while (alive) {
                        Thread.sleep(1000);
                    }
                    log.info("Отключен клиент: {}", incomingSocket.getInetAddress());
                }
            } catch (IOException | InterruptedException e) {
                log.error("Ошибка коммуникации", e);
                throw new RuntimeException(e);
            }
        });
        communicationThread.start();
    }

    public InputStream getInputStream() throws IOException {
        if (socket == null) {
            return null;
        }
        return socket.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        if (socket == null) {
            return null;
        }
        return socket.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        alive = false;
    }
}
