package ru.dnechoroshev.simplecommunicator.debug;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Callable;

@Data
@RequiredArgsConstructor
@Slf4j
public class DummyInputReader implements Callable<File> {

    private final InputStream input;

    private volatile boolean grabInput;

    private volatile int grabDuration;

    @Override
    public File call() throws Exception {

        byte[] buffer = new byte[4096];

        // Skipping any input from caller
        log.info("Skipping any audio input from remote caller");
        while (!grabInput && (input.read(buffer)) != -1);

        log.info("Start audio grabbing");
        return AudioReader.readAudioStreamToFile(input, grabDuration);
    }

    public synchronized void startAudioGrabbinfFor(int duration) {
        this.grabDuration = duration;
        this.grabInput = true;
    }
}
