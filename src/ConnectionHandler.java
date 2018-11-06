import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * ConnectionHandler class handles each seperate connection, processing any requests passed to it.
 */
public class ConnectionHandler implements Runnable {

    private Socket socket;
    private String directory;
    private BufferedReader input;
    private PrintWriter output;
    private BufferedOutputStream buffOut;
    private StringTokenizer tokenizer;
    private StringBuilder headRequest;
    private String data;


    /**
     * ConnectionHandler constructor, creates the buffered in and out readers for receiving and
     * passing data to the client.
     *
     * @param socket    Socket object which the client has connected to
     * @param directory directory of the server
     */
    public ConnectionHandler(Socket socket, String directory) {
        System.out.println(ThreadColor.ANSI_BLUE + "Connection Established, creating handler");
        WebServerMain.printToLog("Connection Established, creating handler");
        this.socket = socket;
        this.directory = directory;

        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            buffOut = new BufferedOutputStream(socket.getOutputStream());

        } catch (IOException e) {
            System.out.println("Connection Handler Constructor: " + e.getMessage());
        }
    }

    /**
     * Overriden run class required for a Class implementing runnable, waits for request,
     * reads the first line and passes data to required methods. Also closes connection and
     * input/output readers when completed.
     */
    @Override
    public void run() {
        try {

            while (true) {
                // Receive the input being sent
                String request = input.readLine();

                // Create a tokenizer for the first line of the request and save needed variables
                tokenizer = new StringTokenizer(request);
                String requestType = tokenizer.nextToken();
                String requestDirectory = directory + tokenizer.nextToken();

                // Print out the first line of the request as tokens to ensure correct data retrieved
                System.out.println(ThreadColor.ANSI_GREEN + "--------- REQUEST -------");
                System.out.println(ThreadColor.ANSI_GREEN + "String 0 = " + requestType);
                System.out.println(ThreadColor.ANSI_GREEN + "String 1 = " + requestDirectory);
                System.out.println(ThreadColor.ANSI_GREEN + "String 2 = " + tokenizer.nextToken());

                // Logs the request recieved to the main server log
                WebServerMain.printToLog("Received " + requestType + " request on file " + requestDirectory);

                handleRequest(requestDirectory, requestType);
                break;
            }
        } catch (IOException e) {
            System.out.println("ClientHandler run Catch: " + e.getMessage());
        } finally {
            try {
                input.close();
                output.close();
                buffOut.close();
                socket.close();
                System.out.println(ThreadColor.ANSI_RED + "Connection Closed");
            } catch (IOException e) {
                System.out.println("ClientHandler run Finally: " + e.getMessage());
            }
        }
    }

    /**
     * Processes the request of the client, calling the relevant retrieval methods if needed
     * or sending back 404 and 501 responses if not valid.
     *
     * @param requestedFile file path of the file declared by the client
     * @param request       HTTP request, ie GET, HEAD, PUT
     */
    private void handleRequest(String requestedFile, String request) {
        String contentType = "";

        File foundFile = getFile(requestedFile);

        // Check to see if the options is wanted for server or file
        if ((request.equals("OPTIONS")) && (requestedFile.endsWith("*"))) {
            request = "SERVEROPTIONS";
        }

        // Try and get the file, if its null, check if its a general options request
        // If it isint then send 404 error.
        if (foundFile != null) {
            // Check what kind of file it is
            contentType = checkContentType(foundFile);
        } else if (!(request.equals("SERVEROPTIONS") || request.equals("PUT"))) {
            request = "404";
        }

        switch (request) {
            case "HEAD":
                sendHEAD(foundFile, contentType, 0);
                break;
            case "GET":
                sendGET(foundFile, contentType);
                break;
            case "OPTIONS":
                sendHEAD(foundFile, contentType, 1);
                break;
            case "SERVEROPTIONS":
                sendHEAD(foundFile, contentType, 2);
                break;
            case "PUT":
                putRequest(requestedFile);
                break;
            case "404":
                output.println("HTTP/1.1 404 Not Found");
                WebServerMain.printToLog("Response from server - 404 Not Found");
                System.out.println(ThreadColor.ANSI_RED + "Response from server - 404 Not Found");
                break;
            default:
                output.println("HTTP/1.1 501 Not Implemented");
                WebServerMain.printToLog("Response from server - 501 Not Implemented");
                System.out.println(ThreadColor.ANSI_RED + "501 Not Implemented");
                break;
        }
    }

    /**
     * Given a directory as a string, converts it to a file object if the file exists.
     *
     * @param requestDirectory string of file path
     * @return null if file does not exist or File object
     */
    private File getFile(String requestDirectory) {
        if (new File(requestDirectory).isFile()) {
            return new File(requestDirectory);
        } else {
            return null;
        }
    }

    /**
     * Gets the length of a file.
     *
     * @param requestedFile File object of the file
     * @return int Length of the file
     */
    private int getFileLength(File requestedFile) {
        long fileLength = requestedFile.length();
        return (int) fileLength;
    }

    /**
     * Given a file, retrieves the extension of the file and returns the HTTP content type.
     *
     * @param file File object of file to check
     * @return String of content type
     */
    private String checkContentType(File file) {
        String fileName = file.getName();

        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".jpg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else {
            return "text/plain";
        }
    }

    /**
     * Given a content type from a client request, returns the extension of the file.
     *
     * @param contentType String content-type header from request
     * @return UNKNOWN if not implemented otherwise file extension
     */
    private String getFileTypeFromContentType(String contentType) {
        if (contentType.endsWith("text/html")) {
            return ".html";
        } else if (contentType.endsWith("image/png")) {
            return ".png";
        } else if (contentType.endsWith("text/plain")) {
            return ".txt";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Sends the requested file data as either text for html and text files or as bytes for
     * images to the client via the socket PrintWriter output or a buffered output for byte data.
     *
     * @param file        File object to retrieve the data from
     * @param contentType Type of file
     */
    private void sendData(File file, String contentType) {

        try {
            if (contentType.equals("text/html") || contentType.equals("text/plain")) {
                Scanner scanner = new Scanner(new File(file.getPath()));
                String text = scanner.useDelimiter("\\A").next();
                output.print(text);
                scanner.close();

            } else {

                byte[] buffer = new byte[getFileLength(file)];
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));

                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    buffOut.write(buffer, 0, bytesRead);
                }

                in.close();
            }

            output.flush();

        } catch (IOException e) {
            System.out.println("ClientHandler sendData: " + e.getMessage());
        }

    }

    /**
     * Sends the HEAD request to the client. Configuration options have been included for
     * processing OPTIONS requests.
     *
     * @param file        File object to return the HEAD data for
     * @param contentType Content type of file
     * @param config      integer used to control OPTIONS requests, 0 = normal HEAD or GET, 1 = OPTIONS,
     *                    2 = SERVER OPTIONS
     */
    private void sendHEAD(File file, String contentType, int config) {
        output.println("HTTP/1.1 200 OK");
        WebServerMain.printToLog("Response from server - 200 OK");
        System.out.println(ThreadColor.ANSI_RED + "Response from server - 200 OK");
        output.println("Server: Java HTTP Server by ACM35");
        output.println("Date: " + new Date());
        if (config != 0) {
            output.println("Allow: OPTIONS, GET, HEAD");
        }
        if (config != 2) {
            output.println("Content-Type: " + contentType);
        }
        if (config == 2) {
            output.println("Content-Length: 0");
        } else {
            output.println("Content-Length: " + getFileLength(file));
        }
        output.println();
        output.flush();
        WebServerMain.printToLog("Header sent from server");
    }

    /**
     * Processes the GET HTTP request sending the head, data and then logging.
     *
     * @param file        File to be sent to the client
     * @param contentType Content type of the file
     */
    private void sendGET(File file, String contentType) {
        // return the header
        sendHEAD(file, contentType, 0);
        // return the content
        sendData(file, contentType);
        WebServerMain.printToLog(contentType + " of length " + getFileLength(file) + " sent from server");
    }

    /**
     * Processes the PUT HTTP request by reading the head and then data of the request before
     * performing neccassary checks and operations of the file in question.
     * Will send 501, 200 or 201 responses to the client.
     *
     * @param requestedFile String of the requested file to alter
     */
    private void putRequest(String requestedFile) {
        String fileType = "";
        try {
            // Create a string builder to save the whole head of request
            headRequest = new StringBuilder();
            String line = null;
            System.out.println(ThreadColor.ANSI_PURPLE + "--------- HEAD -------");
            // Iterate through until there is a blank line signifying start of data
            do {
                line = input.readLine();
                headRequest.append(line + "\n");
                //If the line is the content-type, check what kind of file it is
                if (line.toLowerCase().startsWith("content-type")) {
                    fileType = getFileTypeFromContentType(line);
                }
                System.out.println(ThreadColor.ANSI_PURPLE + line);
            } while (!(line).equals(""));
        } catch (IOException e) {
            System.out.println("putRequest: Read from input error:" + e.getMessage());
        }

        // If the file type is not recognised or server does not have functionality to deal with it
        // will return 501 response.
        if (fileType.equals("UNKNOWN")) {
            output.println("HTTP/1.1 501 NOT IMPLEMENTED");
            WebServerMain.printToLog("Response from server - 501 NOT IMPLEMENTED");
            System.out.println(ThreadColor.ANSI_RED + "Response from server - 501 Not Implemented");
            return;
        }

        // Check if the file already exists
        File targetFile = new File(requestedFile + fileType);
        boolean existsAlready = false;
        if (targetFile.exists() && !targetFile.isDirectory()) {
            existsAlready = true;
        }

        try (
                BufferedWriter bw = new BufferedWriter(new FileWriter(targetFile));
                PrintWriter pw = new PrintWriter(bw)
        ) {
            // Write the data to the file, PUT request always overwrites so OK to do so
            System.out.println(ThreadColor.ANSI_CYAN + "--------- DATA -------");
            StringBuffer s = new StringBuffer("");
            String line = "";
            do {
                line = input.readLine();
                s.append(line + "\n");
                pw.println(line);
                System.out.println(line);
            } while (!(line).equals(""));

            // If the file existed, return 204 message else 201 to acknowledge creation
            if (!existsAlready) {
                output.println("HTTP/1.1 201 Created");
                WebServerMain.printToLog("Response from server - 201 Created");
                System.out.println(ThreadColor.ANSI_RED + "Response from server - 201 Created");
            } else {
                output.println("HTTP/1.1 200 OK");
                WebServerMain.printToLog("Response from server - 200 OK");
                System.out.println(ThreadColor.ANSI_RED + "Response from server - 200 OK");
            }

            output.println("Content-Location: " + targetFile.getPath());


        } catch (IOException e) {
            System.out.println("putRequest: Read from data / write to file error:" + e.getMessage());
        }
    }

}
