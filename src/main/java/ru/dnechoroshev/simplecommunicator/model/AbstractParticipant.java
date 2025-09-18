package ru.dnechoroshev.simplecommunicator.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

@Data
@Slf4j
@EqualsAndHashCode(of = {"name", "port"})
@ToString(of = {"name"})
public abstract class AbstractParticipant implements Closeable {

    protected static final int WAITING_FOR_RESPONSE = 0xFFAA001;
    protected static final int CONNECTION_STARTING = 0xFFAA002;
    protected static final int CONNECTION_TIMEOUT = 0xFFAA003;

    protected static final int TIMEOUT_SECONDS = 15*1000;
    protected static final int CHECK_DELAY_MS = 200;

    // Имя абонента
    protected String name;

    // Вызываемый/вызывающий абонент
    protected AbstractParticipant correspondent;

    // Выделяемый системой порт для текущего абонента
    protected int port;

    protected ServerSocket serverSocket;

    protected Socket socket;

    protected Thread communicationThread;

    protected volatile boolean connected;
    protected volatile boolean ready;

    public AbstractParticipant(String name, int port, AbstractParticipant correspondent) {
        this.name = name;
        this.port = port;
        this.correspondent = correspondent;
        if (correspondent != null) {
            correspondent.setCorrespondent(this);
        }
    }

    public AbstractParticipant(String name, int port) {
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
        log.debug("Ошибка соединения: {}", name);
        connected = false;
    }

    @Override
    public void close() {
        connected = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            log.error("Ошибка закрытия клиента {}", name, e);
        }
    }
}
