
import Server.*;

public class Main {
    private static final int PORT = 9999;

    public static void main(String[] args) {
        final var server = new Server();

        server.addHandler("GET", "/messages", (Handler) (request, responseStream) -> {
            responseStream.write((
                    "HTTP/1.1 201 OK\r\n" +
                            "Content-Type: " + "text/plain" + "\r\n" +
                            "Content-Length: Get Data\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            responseStream.flush();
        });

        server.addHandler("POST", "/messages", (Handler) (request, responseStream) -> {
            responseStream.write((
                    "HTTP/1.1 202 POST Data\r\n" +
                            "Content-Type: " + "text/plain" + "\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            responseStream.flush();
        });
        server.listen(PORT);
    }
}

