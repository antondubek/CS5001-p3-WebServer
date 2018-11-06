import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server class which initiates a server listening on a desired port.
 * It then creates and manages a threadpool which handles each connection to the server.
 * This allows for multiple simultaneous connections.
 */
public class Server implements Runnable {

    private String directory;
    private int port;
    private int poolSize;
    private ExecutorService threadPool;

    /**
     * Server constructor.
     *
     * @param directory directory of the server
     * @param port      port to listen on for incoming connections
     * @param poolSize  size of threadPool, number of simultaneous connections allowed
     */
    public Server(String directory, int port, int poolSize) {
        this.directory = directory;
        this.port = port;
        this.poolSize = poolSize;
        this.threadPool = Executors.newFixedThreadPool(poolSize);
    }

    /**
     * Since the server class implements runnable, a overriden run method must be created which is
     * executed from WebServerMain.
     */
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            while (true) {
                System.out.println(ThreadColor.ANSI_BLUE + "Server Listening on Port " + port);
                WebServerMain.printToLog("Server Listening on Port " + port);
                threadPool.execute(new ConnectionHandler(serverSocket.accept(), directory));
            }

        } catch (IOException e) {
            System.out.println("Server exception " + e.getMessage());
        }
    }
}
