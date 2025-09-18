package ru.dnechoroshev.simplecommunicator.model;

import lombok.*;

import java.io.Closeable;
import java.time.Instant;
import java.util.UUID;

@EqualsAndHashCode(of = {"callId"})
@ToString
public class CallSession implements Closeable {

    @Getter
    private String callId;

    @Getter
    private AbstractParticipant caller;

    @Getter
    @Setter
    private AbstractParticipant callee;

    @Getter
    @Setter
    private Instant creationTime;

    @Getter
    @Setter
    private boolean alive;

    public CallSession(@NonNull AbstractParticipant caller, @NonNull AbstractParticipant callee) {
        this.callId = UUID.randomUUID().toString();
        this.caller = caller;
        this.callee = callee;
        this.creationTime = Instant.now();
        this.alive = true;
    }

    @Override
    public void close() {
        if (caller != null) {
            caller.close();
        }
        if (callee != null) {
            callee.close();
        }
        alive = false;
    }
}
