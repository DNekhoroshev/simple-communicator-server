package ru.dnechoroshev.simplecommunicator.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;

public class AudioPlayer {

    private final Logger log = LoggerFactory.getLogger(AudioPlayer.class);

    private AudioFormat audioFormat;
    private SourceDataLine sourceDataLine;
    private boolean isPlaying;

    public AudioPlayer() {
        // Формат аудио: 44.1kHz, 16-bit, стерео (стандартный для воспроизведения)
        audioFormat = new AudioFormat(16000.0f, 16, 1, true, false);
    }

    public AudioPlayer(AudioFormat format) {
        this.audioFormat = format;
    }

    /**
     * Инициализирует линию вывода звука (наушники/колонки)
     */
    public boolean initializeAudioOutput() {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

            if (!AudioSystem.isLineSupported(info)) {
                log.error("Аудио выход с таким форматом не поддерживается");
                return false;
            }

            sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceDataLine.open(audioFormat);
            return true;

        } catch (LineUnavailableException e) {
            log.error("Аудио выход недоступен: " + e.getMessage());
            return false;
        }
    }

    /**
     * Воспроизводит массив байтов как аудио
     */
    public void playAudio(byte[] audioData) {
        if (sourceDataLine == null) {
            if (!initializeAudioOutput()) {
                return;
            }
        }

        try {
            sourceDataLine.start();
            sourceDataLine.write(audioData, 0, audioData.length);
            sourceDataLine.drain(); // Ждем завершения воспроизведения
            sourceDataLine.stop();

        } catch (Exception e) {
            log.error("Ошибка при воспроизведении: {}", e.getMessage());
        }
    }

    /**
     * Воспроизводит аудио из InputStream в реальном времени
     */
    public void playStream(InputStream audioStream) {
        if (sourceDataLine == null) {
            if (!initializeAudioOutput()) {
                return;
            }
        }

        isPlaying = true;

        try {
            sourceDataLine.start();

            byte[] buffer = new byte[4096];
            int bytesRead;

            while (isPlaying && (bytesRead = audioStream.read(buffer)) != -1) {
                if (bytesRead > 0) {
                    sourceDataLine.write(buffer, 0, bytesRead);
                }
            }

            sourceDataLine.drain();
            sourceDataLine.stop();

        } catch (IOException e) {
            log.error("Ошибка при чтении потока: {}", e.getMessage());
        } finally {
            stopPlayback();
        }
    }

    /**
     * Останавливает воспроизведение
     */
    public void stopPlayback() {
        isPlaying = false;
        if (sourceDataLine != null && sourceDataLine.isOpen()) {
            sourceDataLine.stop();
            sourceDataLine.close();
        }
    }

    /**
     * Проверяет, идет ли воспроизведение
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Возвращает текущий аудио формат
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }
}
