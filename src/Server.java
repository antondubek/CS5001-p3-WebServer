import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private String directory;
    private int port;
    private int poolSize;
    private ExecutorService threadPool;

    public Server(String directory, int port, int poolSize) {
        this.directory = directory;
        this.port = port;
        this.poolSize = poolSize;
        this.threadPool = Executors.newFixedThreadPool(poolSize);
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout(30*1000);

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
