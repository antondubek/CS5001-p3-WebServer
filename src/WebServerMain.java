import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WebServerMain {
    public static String logFilePath = "logFile.txt";

    public static void main(String[] args) {

        // Check the arguments
        if (args.length == 2) {
            String directory = args[0];
            int port = Integer.parseInt(args[1]);

            createLogFile(logFilePath);

            Server server = new Server(directory, port, 10);
            new Thread(server).start();

        } else {
            System.out.println("Usage: java WebServerMain <document_root> <port>");
        }
    }

    private static void createLogFile(String filePath) {
        File logFile = new File(filePath);
        FileWriter logFileWriter = null;
        try {
            if (logFile.createNewFile()) {
                // file created
            }
            printToLog("**************************************************");
            printToLog("Server Started");

        } catch (IOException e) {
            System.out.println("Log file creation exception: " + e.getMessage());
        }

    }

    public static void printToLog(String message){
        try {
            FileWriter logFileWriter = new FileWriter(logFilePath, true);
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            logFileWriter.write(dateFormat.format(date) + " : " + message + "\n");
            logFileWriter.flush();
            logFileWriter.close();

        } catch (IOException e) {
            System.out.println("Print to Log file exception: " + e.getMessage());
        }

    }
}
