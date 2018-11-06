import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main WebServer class, running this with the arguments listed below will create the server and logs
 * Argument 1 : String : directory of the files you wish to access or insert to (Must exist)
 * Argument 2 : int : Port number which the server will listen on (1024 - 49151)
 */
public class WebServerMain {
    public static String logFilePath = "logFile.txt";

    public static void main(String[] args) {

        // Check the arguments
        if (args.length == 2) {

            String directory = args[0];
            int port = Integer.parseInt(args[1]);

            createLogFile(logFilePath);

            // Check the arguments passed are valid
            if(checkDirectory(directory) && checkPort(port)){
                Server server = new Server(directory, port, 10);
                new Thread(server).start();
            }

        } else {
            // Error message
            System.out.println("Usage: java WebServerMain <document_root> <port>");
        }
    }

    /**
     * Checks whether the directory exists
     * @param directory String containing location of directory
     * @return Boolean True if exists, false if not
     */
    private static boolean checkDirectory(String directory){
        File file = new File(directory);
        if(file.isDirectory()){
            return true;
        } else{
            System.out.println("ERROR STARTING SERVER: Please ensure that the directory exists");
            return false;
        }
    }

    /**
     * Checks the port number passed is valid (ie within the registered port range 1024 - 49151)
     * @param port int number to listen on
     * @return boolean true if in range, false if not
     */
    private static boolean checkPort(int port){
        if(port <= 49151 && port > 1024){
            return true;
        } else {
            System.out.println("ERROR STARTING SERVER: Please use a port number from 1024 - 49151");
            return false;
        }
    }

    /**
     * Checks whether a log file exists, if it does, loads it, else creates one
     * @param filePath name of the log file
     */
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

    /**
     * Simple method to log a message to the log file
     * @param message message to put into the log file.
     */
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
