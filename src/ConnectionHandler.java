import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;


public class ConnectionHandler extends Thread {

    private Socket socket;
    private String directory;
    private int helper;
    private BufferedReader input;
    private PrintWriter output;
    private BufferedOutputStream buffOut;
    private File requestedFile;

    public ConnectionHandler(Socket socket, String directory) {
        System.out.println(ThreadColor.ANSI_BLUE + "Connection Established, creating handler");
        this.socket = socket;
        this.directory = directory;
        this.helper = 0;
    }

    @Override
    public void run() {
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            buffOut = new BufferedOutputStream(socket.getOutputStream());

            while (true) {


                // Receive the input being sent
                String request = input.readLine();

                //Parse request
                String[] parsedRequest = request.split(" ");

                System.out.println(ThreadColor.ANSI_GREEN + "String 0 = " + parsedRequest[0]);
                String requestType = parsedRequest[0];
                System.out.println(ThreadColor.ANSI_GREEN + "String 1 = " + parsedRequest[1]);
                String requestDirectory = directory + parsedRequest[1];

                if (new File(requestDirectory).isFile()) {
                    requestedFile = new File(requestDirectory);
                    handleRequest(requestedFile, requestType);
                } else {
                    // 404 message returned
                    output.println("HTTP/1.1 404 Not Found");
                    break;
                }


            }
        } catch (IOException e) {
            System.out.println("Ooops" + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                //oh well
            }
        }
    }

    private void handleRequest(File requestedFile, String request){
        // Check what kind of file it is
        String contentType = checkContentType(requestedFile);

        switch(request){
            case "HEAD":
                sendHEAD(requestedFile, contentType);
                break;
            case "GET":
                sendGET(requestedFile, contentType);
                break;
            default:
                output.println("HTTP/1.1 501 Not Found");
                break;
        }
    }

    private int getFileLength(File requestedFile) {
        long fileLength = requestedFile.length();
        return (int) fileLength;
    }


    private String getData(File requestedFile) {
        try {
            Document doc = Jsoup.parse(requestedFile, "UTF-8");
            return doc.html();
        } catch (IOException e) {
            System.out.println("Get Data Exception: " + e.getMessage());
        }
        return "";
    }

    private String checkContentType(File file) {
        String fileName = file.getName();
        if (fileName.endsWith(".htm") || fileName.endsWith("html")) {
            return "text/html";
        } else if (fileName.endsWith(".jpg")) {
            return "image/jpeg";
        } else {
            return "text/plain";
        }
    }

    private void sendHEAD(File file, String contentType) {
        output.println("HTTP/1.1 200 OK");
        output.println("Server: Java HTTP Server by ACM35 : 1.0");
        output.println("Date: " + new Date());
        output.println("Content-type: " + contentType);
        output.println("Content-length: " + getFileLength(file));
        output.println();
        output.flush();
    }

    private void sendGET(File file, String contentType) {
        // return the header + file
        output.println("HTTP/1.1 200 OK");
        output.println("Server: Java HTTP Server by ACM35 : 1.0");
        output.println("Date: " + new Date());
        output.println("Content-type: " + contentType);
        output.println("Content-length: " + getFileLength(file));
        output.println();
        output.flush();

        try {
            if (contentType != "image/jpeg") {
                //output.println(getData(file));

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
                output.println(getData(file));

            }
        } catch (IOException e) {
            System.out.println("SEND GET IMAGE EXCEPTION: " + e.getMessage());
        }

        output.flush();
    }

}
