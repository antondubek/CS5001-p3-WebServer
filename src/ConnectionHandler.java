import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;


public class ConnectionHandler implements Runnable {

    private Socket socket;
    private String directory;
    private BufferedReader input;
    private PrintWriter output;
    private BufferedOutputStream buffOut;
    private StringTokenizer tokenizer;
    private StringBuilder headRequest;

    private String data;

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

                System.out.println(ThreadColor.ANSI_GREEN + "String 0 = " + requestType);
                System.out.println(ThreadColor.ANSI_GREEN + "String 1 = " + requestDirectory);
                System.out.println(ThreadColor.ANSI_GREEN + "String 2 = " + tokenizer.nextToken());
                //System.out.println(ThreadColor.ANSI_BLUE + headRequest.toString());
//                System.out.println(ThreadColor.ANSI_CYAN + "DATA = " + data);

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

    private File getFile(String requestDirectory) {
        if (new File(requestDirectory).isFile()) {
            return new File(requestDirectory);
        } else {
            return null;
        }
    }

    private int getFileLength(File requestedFile) {
        long fileLength = requestedFile.length();
        return (int) fileLength;
    }

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

    private String getFileTypeFromContentType(String contentType){
        if(contentType.endsWith("text/html")){
            return ".html";
        }  else if(contentType.endsWith("image/png")){
            return ".png";
        } else if(contentType.endsWith("text/plain")){
            return ".txt";
        } else {
            return "UNKNOWN";
        }
    }

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


    private void sendGET(File file, String contentType) {
        // return the header
        sendHEAD(file, contentType, 0);
        // return the content
        sendData(file, contentType);
        WebServerMain.printToLog(contentType + " of length " + getFileLength(file) + " sent from server");
    }

    private void putRequest(String requestedFile) {
        String fileType = "";
        try {
            // Create a string builder to save the whole head of request
            headRequest = new StringBuilder();
            String line = null;
            System.out.println(ThreadColor.ANSI_PURPLE + "--------- HEAD -------");
            // Iterate through until there is a blank line signifying start of data
            while (!(line = input.readLine()).equals("")) {
                headRequest.append(line + "\n");
                //If the line is the content-type, check what kind of file it is
                if (line.toLowerCase().startsWith("content-type")) {
                    fileType = getFileTypeFromContentType(line);
                }
                System.out.println(ThreadColor.ANSI_PURPLE + line);
            }
        } catch (IOException e) {
            System.out.println("putRequest: Read from input error:" + e.getMessage());
        }

        if(fileType.equals("UNKNOWN")){
            output.println("HTTP/1.1 501 NOT IMPLEMENTED");
            WebServerMain.printToLog("Response from server - 501 NOT IMPLEMENTED");
            System.out.println(ThreadColor.ANSI_RED + "Response from server - 501 Not Implemented");
            return;
        }

        // Check if the file already exists
        File targetFile = new File(requestedFile + fileType);
        boolean existsAlready = false;
        if(targetFile.exists() && !targetFile.isDirectory()){
            existsAlready = true;
        }

        try(
                BufferedWriter bw = new BufferedWriter(new FileWriter(targetFile));
                PrintWriter pw = new PrintWriter(bw)
        ){
            // Write the data to the file, PUT request always overwrites so OK to do so
            System.out.println(ThreadColor.ANSI_CYAN + "--------- DATA -------");
            StringBuffer s = new StringBuffer("");
            String line = "";
            while (!(line = input.readLine()).equals("")) {
                s.append(line + "\n");
                pw.println(line);
                System.out.println(line);
            }

            // If the file existed, return 201 message else 204 to acknowledge competition
            if(existsAlready){
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
