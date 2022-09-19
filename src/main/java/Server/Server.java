package Server;

import java.io.*;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private static final int SIZE_POOL = 64;

    public void listen(int port) {
        ExecutorService executorService = Executors.newFixedThreadPool(SIZE_POOL);
        try (final var serverSocket = new ServerSocket(port)) {
            final var handler = new Handler();
            while (true) {
                try {
                    final var socket = serverSocket.accept();
                    System.out.println("Server is listening");
                    executorService.execute(() -> handler.handle(socket));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}