package com.vanta.client;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Consumer;

final class VantaRadio {
    private static volatile boolean running;
    private static volatile SourceDataLine activeLine;
    private static volatile Thread worker;
    private static volatile long generation;

    private VantaRadio() {
    }

    static synchronized boolean isRunning() {
        return running;
    }

    static synchronized void start(String address, Consumer<String> status) {
        stop();
        if (address == null || address.isBlank()) {
            status.accept("Vanta Radio: enter a direct WAV or AIFF stream URL first.");
            return;
        }
        long session = ++generation;
        running = true;
        worker = new Thread(() -> stream(address.trim(), status, session), "Vanta Radio");
        worker.setDaemon(true);
        worker.start();
    }

    static synchronized void stop() {
        generation++;
        running = false;
        SourceDataLine line = activeLine;
        activeLine = null;
        if (line != null) {
            line.stop();
            line.flush();
            line.close();
        }
        Thread thread = worker;
        worker = null;
        if (thread != null) thread.interrupt();
    }

    private static void stream(String address, Consumer<String> status, long session) {
        SourceDataLine line = null;
        try {
            URLConnection connection = new URL(address).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setUseCaches(false);
            try (InputStream input = connection.getInputStream();
                 AudioInputStream audio = AudioSystem.getAudioInputStream(input)) {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audio.getFormat());
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(audio.getFormat());
                activeLine = line;
                line.start();
                status.accept("Vanta Radio: playing configured stream.");
                byte[] buffer = new byte[8192];
                int count;
                while (running && generation == session && (count = audio.read(buffer, 0, buffer.length)) >= 0) {
                    if (count > 0) line.write(buffer, 0, count);
                }
                if (running && generation == session) status.accept("Vanta Radio: stream ended.");
            }
        } catch (Exception exception) {
            if (running && generation == session) {
                status.accept("Vanta Radio: could not play this URL (use a direct PCM WAV/AIFF stream).");
            }
        } finally {
            if (generation == session) running = false;
            if (line != null) {
                line.stop();
                line.flush();
                line.close();
            }
            if (activeLine == line) activeLine = null;
            if (worker == Thread.currentThread()) worker = null;
        }
    }
}

