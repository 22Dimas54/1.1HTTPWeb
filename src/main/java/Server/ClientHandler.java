package Server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class ClientHandler implements Runnable {
    private Server server;
    private Socket socket;

    public ClientHandler(Socket socket, Server server) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final var out = new BufferedOutputStream(socket.getOutputStream());
            final String requestLine = in.readLine();
            final var parts = requestLine.split(" ");
            server.matchingParameters(parts, socket);
            final var path = parts[1];
            final List validPaths = server.getValidPaths();
            if (!validPaths.contains(path)) {
                server.failedConnection(out);
            }
            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);
            if (path.equals("/classic.html")) {
                server.successfulConnectionWithLabel(out, filePath, mimeType);
            }
            server.successfulConnection(out, filePath, mimeType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
