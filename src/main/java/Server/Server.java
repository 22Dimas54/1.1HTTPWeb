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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private static final int SIZE_POOL = 64;
    private static final String PUBLIC_PATH = "public";
    private static final int COUNT_PARAMETERS = 3;
    private List validPaths;
    public static final String GET = "GET";
    public static final String POST = "POST";
    private static final List allowedMethods = List.of(GET, POST);

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> allHandlers = new ConcurrentHashMap<>();

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
        try (final var in = new BufferedInputStream(socket.getInputStream());
             final var out = new BufferedOutputStream(socket.getOutputStream());
             socket) {
            // лимит на request line + заголовки
            final var limit = 4096;

            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);

            // ищем request line
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                badRequest(out);
                return;
            }

            // читаем request line
            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                badRequest(out);
                return;
            }

            final var method = requestLine[0];
            if (!allowedMethods.contains(method)) {
                badRequest(out);
                return;
            }
            System.out.println(method);

            final var path = requestLine[1];
            if (!path.startsWith("/")) {
                badRequest(out);
                return;
            }
            System.out.println(path);

            // ищем заголовки
            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                badRequest(out);
                return;
            }

            // отматываем на начало буфера
            in.reset();
            // пропускаем requestLine
            in.skip(headersStart);

            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
            final var request = new Request(method, headers, null, path);

            System.out.println(request);

            // для GET тела нет
            if (!method.equals(GET)) {
                in.skip(headersDelimiter.length);
                // вычитываем Content-Length, чтобы прочитать body
                final var contentLength = extractHeader(headers, "Content-Length");
                if (contentLength.isPresent()) {
                    final var length = Integer.parseInt(contentLength.get());
                    final var bodyBytes = in.readNBytes(length);

                    final var body = new String(bodyBytes);
                    System.out.println(body);
                }
            }

            if (validPaths.contains(request.getPath()) ) {
                final var filePath = Path.of(".", PUBLIC_PATH, request.getPath());
                final var mimeType = Files.probeContentType(filePath);
                if (path.equals("/classic.html")) {
                    successfulConnectionWithLabel(out, filePath, mimeType);
                } else {
                    successfulConnection(out, filePath, mimeType);
                }
            }

            var methodMap = allHandlers.get(request.getMethod());
            if (methodMap==null) {
                notFound(out);
                return;
            }
            var handler = methodMap.get(request.getPath());
            if (handler==null) {
                notFound(out);
                return;
            }

            handler.handle(request,out);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String method, String path, Handler handler) {
        var methodMap = allHandlers.get(method);
        if (methodMap == null) {
            methodMap = new ConcurrentHashMap<>();
            allHandlers.put(method, methodMap);
        }
        methodMap.put(path, handler);
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private static void notFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
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