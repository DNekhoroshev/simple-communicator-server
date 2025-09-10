package ru.dnechoroshev.simplecommunicator.debug;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Data
@Slf4j
public class AudioReader {
    public static File readAudioStreamToFile(InputStream inputStream, long timeout) {

        FileOutputStream fos = null;
        try {
            Path tempFile = Files.createTempFile("testmessage", ".tmp");
            log.info("Временный файл создан: {}", tempFile);
            log.info("Прием тестового сообщения");
            fos = new FileOutputStream(tempFile.toFile());
            byte[] buffer = new byte[4096];
            int bytesRead;
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;
            while (elapsedTime < timeout && (bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                elapsedTime = System.currentTimeMillis() - startTime;
            }
            return tempFile.toFile();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
