package ru.dnechoroshev.simplecommunicator.server;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.dnechoroshev.simplecommunicator.model.Participant;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.*;

public class Communication implements Closeable {

    private final Logger log = LoggerFactory.getLogger(Communication.class);

    @Getter
    private final Participant caller;
    @Getter
    private final Participant callee;

    private ThreadPoolExecutor dispatchPool =  (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    private volatile boolean live;

    public Communication(Participant caller, Participant callee) {
        this.caller = caller;
        this.callee = callee;
        this.live = true;
    }

    public void touchCaller() {
        dispatchPool.submit(new CallerHandler(caller));
    }

    public void touchCallee() {
        dispatchPool.submit(new CallerHandler(callee));
    }

    public void connectParticipants() {

        try {
            if (!dispatchPool.awaitTermination(15, TimeUnit.SECONDS)) {
                throw new RuntimeException("Таймаут синхронизации абонентов");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        log.info("{} -> {} : Соединение установлено", caller.getName(), callee.getName());

        dispatchPool.submit(() -> {
            try (var callerIn = new BufferedInputStream(caller.getInputStream()); var calleeOut = new BufferedOutputStream(callee.getOutputStream());) {
                moveBytes(new byte[4096], callerIn, calleeOut);
            } catch (IOException e) {
                processError(e);
            }
        });

        dispatchPool.submit(() -> {
            try (var calleeIn = new BufferedInputStream(callee.getInputStream()); var callerOut = new BufferedOutputStream(caller.getOutputStream());) {
                moveBytes(new byte[4096], calleeIn, callerOut);
            } catch (IOException e) {
                processError(e);
            }
        });
    }

    private void moveBytes(byte[] buffer, BufferedInputStream in, BufferedOutputStream out) {
        int bytesRead;
        try {
            while (live && (bytesRead = in.read(buffer)) != -1) {
                if (bytesRead > 0) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            log.debug("Ошибка разрыва соединения", e);
            log.info("Соединение разорвано");
        } finally {
            live = false;
        }
    }

    private void processError(Exception e) {
        log.error("Ошибка соединения");
        live = false;
        throw new RuntimeException(e);
    }

    @Override
    public void close() {
        log.info("{} -> {} : Закрываем соединение", caller.getName(), callee.getName());
        this.live = false;
        dispatchPool.shutdown();
        log.info("{} -> {} : Соединение закрыто", caller.getName(), callee.getName());
    }
}
