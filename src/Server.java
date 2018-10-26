import java.io.IOException;
import java.net.ServerSocket;

public class Server {

    private String directory;
    private int port;

    public Server(String directory, int port) {
        this.directory = directory;
        this.port = port;

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            while (true) {
                System.out.println(ThreadColor.ANSI_BLUE + "Server Listening on Port " + port);
                new ConnectionHandler(serverSocket.accept(), directory).start();
            }

        } catch (IOException e) {
            System.out.println("Server exception " + e.getMessage());
        }

    }


}
