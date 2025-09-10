package ru.dnechoroshev.simplecommunicator.debug;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Slf4j
public class DummyTestAudioSender {

    public static void sendAudioFileToOut(String fileName, OutputStream outputStream) {
        final ClassPathResource resource = new ClassPathResource(fileName);
        try (InputStream audioIn = resource.getInputStream()) {
            sendBytesToOut(audioIn.readAllBytes(), outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendBytesToOut(byte[] bytes, OutputStream outputStream) {
        try {
            log.info("Передача тестового сообщения");
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            byteArrayInputStream.transferTo(outputStream);
            Thread.sleep(5000);
        } catch (IOException e) {
            log.error("Ошибка передачи данных удаленному клиенту", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
