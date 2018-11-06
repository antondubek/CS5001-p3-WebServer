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

    private BufferedInputStream putBuff;

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
                //System.out.println(input.readLine());

//                StringBuilder sb = new StringBuilder();
//
//                String line = null;
//                while (( line = input.readLine()) != null) {
//                    sb.append(line + "\n");
//                }
//
//                System.out.println("here");
//                input.close();
//                output.println("HTTP/1.1 201 Created");
//                output.println("Content-Location: ");
//
//                System.out.println(sb.toString());

                tokenizer = new StringTokenizer(request);
                String requestType = tokenizer.nextToken();
                String requestDirectory = directory + tokenizer.nextToken();

                System.out.println(ThreadColor.ANSI_GREEN + "String 0 = " + requestType);
                System.out.println(ThreadColor.ANSI_GREEN + "String 1 = " + requestDirectory);
                System.out.println(ThreadColor.ANSI_GREEN + "String 2 = " + tokenizer.nextToken());
                //System.out.println(ThreadColor.ANSI_GREEN + "String 3 = " + tokenizer.nextToken());

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
                break;
            default:
                output.println("HTTP/1.1 501 Not Implemented");
                WebServerMain.printToLog("Response from server - 501 Not Implemented");
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
        try {

            BufferedWriter bw = new BufferedWriter(new FileWriter("helloworld.txt"));
            PrintWriter pw = new PrintWriter(bw);

            StringBuffer s = new StringBuffer("");
            String line = "";
            for(int i = 0; i<20; i++){
                s.append(input.readLine());
            }

            System.out.println(s.toString());

//            String line;
//            while ((line = input.readLine()) != null) {
//                pw.println(line);
//            }
            //String line = "";
            String message = "";
            int helper = 0;
//            while (true) {
//                line = input.readLine();
//                System.out.println("/" + line + "/");
//                System.out.println(line.length());
//                System.out.println("Helper = " + helper);
////                if (helper == 1) {
////                    message = line;
////                    System.out.println("Message " + line);
////                    break;
////                }
////                if (line.length() == 0) {
////                    helper++;
////                }
//
//                pw.println(line);
//            }

            System.out.println("PLease be this" + message);

            System.out.println("HERE");
            output.println("HTTP/1.1 201 Created");
            output.println("Content-Location: /helloworld.txt");

            bw.close();
            pw.close();

        } catch (
                IOException e)

        {
            System.out.println("Error" + e.getMessage());
        }
        //first check if it exists

        //if it does exist
        // overwrite it
        // return 200 OK

        // else create it with the content
        // return 201 Created

        //if for some reason this doesnt work, send an error code
        // eg 501 not implemented or something more helpful

    }

}
