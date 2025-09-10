package ru.dnechoroshev.simplecommunicator.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

@Data
@Slf4j
@EqualsAndHashCode(of = {"name", "port"})
public abstract class Participant implements Closeable {

    protected static final int WAITING_FOR_RESPONSE = 0xFFAA001;
    protected static final int CONNECTION_STARTING = 0xFFAA002;
    protected static final int CONNECTION_TIMEOUT = 0xFFAA003;

    protected static final int TIMEOUT_SECONDS = 15*1000;
    protected static final int CHECK_DELAY_MS = 200;

    // Имя абонента
    protected String name;

    // Вызываемый/вызывающий абонент
    protected Participant correspondent;

    // Выделяемый системой порт для текущего абонента
    protected int port;

    protected Socket socket;

    protected Thread communicationThread;

    protected volatile boolean connected;
    protected volatile boolean ready;

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

    public abstract void connect();

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

    protected void processError(Exception e) {
        log.error("Ошибка соединения: {}", name);
        connected = false;
        throw new RuntimeException(e);
    }

    @Override
    public void close() {
        connected = false;
        try {
            socket.close();
        } catch (IOException e) {
            log.error("Ошибка закрытия клиента {}", name, e);
        }
    }
}
