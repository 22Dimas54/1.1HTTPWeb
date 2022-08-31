package Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private static final int SIZE_POOL = 64;
    private static final String PUBLIC_PATH = "public";
    private static final int COUNT_PARAMETERS = 3;
    private List validPaths;

    public void listen(int port) {
        ExecutorService executorService = Executors.newFixedThreadPool(SIZE_POOL);
        try (final var serverSocket = new ServerSocket(port)) {
            fillValidPath();
            while (true) {
                try {
                    final var socket = serverSocket.accept();
                    System.out.println("Server is listening");
                    executorService.execute(() -> handle(socket));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fillValidPath() {
        validPaths = new ArrayList();
        Arrays.asList(new File(PUBLIC_PATH).listFiles()).stream()
                .forEach(e -> validPaths.add("/" + e.getName()));
    }

    private void handle(Socket socket) {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream());
             socket) {
            final String requestLine = in.readLine();
            System.out.printf("Поток %s обрабатывает запрос %s", Thread.currentThread().getName(), requestLine);
            final var parts = requestLine.split(" ");
            if (parts.length != COUNT_PARAMETERS) {
                return;
            }
            final var path = parts[1];
            if (!validPaths.contains(path) || requestLine.equals("favicon.ico")) {
                failedConnection(out);
                return;
            }
            final var filePath = Path.of(".", PUBLIC_PATH, path);
            final var mimeType = Files.probeContentType(filePath);
            if (path.equals("/classic.html")) {
                successfulConnectionWithLabel(out, filePath, mimeType);
            } else {
                successfulConnection(out, filePath, mimeType);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void failedConnection(BufferedOutputStream out) {
        try {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void successfulConnectionWithLabel(BufferedOutputStream out, Path filePath, String mimeType) {
        try {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void successfulConnection(BufferedOutputStream out, Path filePath, String mimeType) {
        try {
            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

}