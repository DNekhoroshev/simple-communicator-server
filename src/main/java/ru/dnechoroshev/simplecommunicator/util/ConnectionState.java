package ru.dnechoroshev.simplecommunicator.util;

import lombok.Getter;

public enum ConnectionState {
    WAITING_FOR_RESPONSE(0xFFAA001), CONNECTION_STARTING(0xFFAA002), CONNECTION_TIMEOUT(0xFFAA003);

    @Getter
    private int state;

    ConnectionState(int state) {
        this.state = state;
    }
}
