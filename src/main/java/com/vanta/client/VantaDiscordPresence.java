package com.vanta.client;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

final class VantaDiscordPresence {
    private static volatile boolean running;
    private static volatile long generation;
    private static volatile RandomAccessFile pipe;
    private static volatile Thread worker;

    private VantaDiscordPresence() {
    }

    static synchronized boolean isRunning() {
        return running;
    }

    static synchronized void start(String applicationId, String status, Consumer<String> feedback) {
        stop();
        if (applicationId == null || !applicationId.matches("[0-9]{8,25}")) {
            feedback.accept("Vanta Presence: enter a numeric Discord application ID first.");
            return;
        }
        long session = ++generation;
        running = true;
        worker = new Thread(() -> connectAndPublish(applicationId, status, feedback, session),
                "Vanta Discord Presence");
        worker.setDaemon(true);
        worker.start();
    }

    static synchronized void stop() {
        generation++;
        running = false;
        RandomAccessFile current = pipe;
        pipe = null;
        if (current != null) {
            try {
                current.close();
            } catch (IOException ignored) {
            }
        }
        Thread currentWorker = worker;
        worker = null;
        if (currentWorker != null) currentWorker.interrupt();
    }

    private static void connectAndPublish(String appId, String status, Consumer<String> feedback, long session) {
        RandomAccessFile connection = null;
        try {
            for (int index = 0; index < 10 && running && generation == session; index++) {
                try {
                    connection = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + index, "rw");
                    break;
                } catch (IOException ignored) {
                }
            }
            if (connection == null) throw new IOException("Discord IPC pipe not found");
            pipe = connection;
            writeFrame(connection, 0, "{\"v\":1,\"client_id\":\"" + escape(appId) + "\"}");
            readFrame(connection);
            String payload = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":"
                    + ProcessHandle.current().pid() + ",\"activity\":{\"details\":\"Playing Minecraft\","
                    + "\"state\":\"" + escape(status == null ? "Vanta Client" : status) + "\","
                    + "\"assets\":{\"large_text\":\"Vanta Client\"},\"instance\":false}},"
                    + "\"nonce\":\"" + UUID.randomUUID() + "\"}";
            writeFrame(connection, 1, payload);
            feedback.accept("Vanta Presence: connected to Discord Rich Presence.");
            while (running && generation == session) {
                Frame frame = readFrame(connection);
                if (frame.opcode == 3) writeFrame(connection, 4, frame.payload);
            }
        } catch (Exception exception) {
            if (running && generation == session) {
                feedback.accept("Vanta Presence: Discord is not running or its IPC connection failed.");
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException ignored) {
                }
            }
            if (pipe == connection) pipe = null;
            if (generation == session) running = false;
            if (worker == Thread.currentThread()) worker = null;
        }
    }

    private static void writeFrame(RandomAccessFile pipe, int opcode, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        writeIntLittleEndian(pipe, opcode);
        writeIntLittleEndian(pipe, bytes.length);
        pipe.write(bytes);
    }

    private static Frame readFrame(RandomAccessFile pipe) throws IOException {
        int opcode = readIntLittleEndian(pipe);
        int length = readIntLittleEndian(pipe);
        if (length < 0 || length > 1_048_576) throw new IOException("Invalid Discord IPC frame");
        byte[] bytes = new byte[length];
        pipe.readFully(bytes);
        return new Frame(opcode, new String(bytes, StandardCharsets.UTF_8));
    }

    private static void writeIntLittleEndian(RandomAccessFile stream, int value) throws IOException {
        stream.write(value & 0xff);
        stream.write((value >>> 8) & 0xff);
        stream.write((value >>> 16) & 0xff);
        stream.write((value >>> 24) & 0xff);
    }

    private static int readIntLittleEndian(RandomAccessFile stream) throws IOException {
        return stream.readUnsignedByte() | (stream.readUnsignedByte() << 8)
                | (stream.readUnsignedByte() << 16) | (stream.readUnsignedByte() << 24);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", " ").replace("\n", " ");
    }

    private record Frame(int opcode, String payload) {
    }
}

