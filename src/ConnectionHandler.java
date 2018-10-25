import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.Socket;
import java.util.Date;

public class ConnectionHandler extends Thread {

    private Socket socket;
    private String directory;
    private int helper;
    private BufferedReader input;
    private PrintWriter output;
    private BufferedOutputStream buffOut;

    public ConnectionHandler(Socket socket, String directory){
        System.out.println(ThreadColor.ANSI_BLUE+"Connection Established, creating handler");
        this.socket = socket;
        this.directory = directory;
        this.helper = 0;
    }

    @Override
    public void run() {
        try{
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            buffOut = new BufferedOutputStream(socket.getOutputStream());

            while(true){
                // Receive the input being sent
                String request = input.readLine();

                // Only get the first parameters passed
                if(helper == 0) {
                    helper++;

                    //Parse request
                    String[] parsedRequest = request.split(" ");

                    System.out.println(ThreadColor.ANSI_GREEN+"String 0 = " + parsedRequest[0]);
                    String requestType = parsedRequest[0];
                    System.out.println(ThreadColor.ANSI_GREEN+"String 1 = " + parsedRequest[1]);
                    String requestDirectory = directory + parsedRequest[1];

                    File requestedFile;

                    if(new File(requestDirectory).isFile()) {
                        requestedFile = new File(requestDirectory);
                    } else {
                        // 404 message returned
                        output.println("HTTP/1.1 404 FILE NOT FOUND");
                        break;
                    }

                    // Check what kind of file it is
                    String contentType = checkContentType(requestedFile);

                    if(requestType.equals("HEAD")){
                        sendHEAD(requestedFile, contentType);
                    }

                    if(requestType.equals("GET")){ // check if it is GET
                        if(contentType.equals("image/jpeg")){
                            sendGETImage(requestedFile, contentType);
                        } else {
                            //Send GET Data
                            sendGET(requestedFile, contentType);
                        }
                    }
                }
            }
        } catch(IOException e){
            System.out.println("Ooops" + e.getMessage());
        } finally {
            try{
                socket.close();
            } catch(IOException e){
                //oh well
            }
        }
    }

    private void getHeader(String requestedDirectory){
        File input = new File(requestedDirectory);
        try{
            Document doc = Jsoup.parse(input, "UTF-8");
            Element header = doc.head();
            //System.out.println(header);

        } catch(IOException e){
            System.out.println(ThreadColor.ANSI_RED+"Header read exception: "+e.getMessage());
        }

        System.out.println(requestedDirectory);
    }

    private int getFileLength(File requestedFile){
        long fileLength = requestedFile.length();
        return (int)fileLength;
    }


    private String getData(File requestedFile){
        try {
            Document doc = Jsoup.parse(requestedFile, "UTF-8");
            return doc.html();
        } catch (IOException e){
            System.out.println("Get Data Exception: " + e.getMessage());
        }
        return "";
    }

    private String checkContentType(File file){
        String fileName = file.getName();
        if(fileName.endsWith(".htm") || fileName.endsWith("html")){
            return "text/html";
        } else if(fileName.endsWith(".jpg")){
            return "image/jpeg";
        } else {
            return "text/plain";
        }
    }

    private void sendHEAD(File file, String contentType){
        output.println("HTTP/1.1 200 OK");
        output.println("Server: Java HTTP Server by ACM35 : 1.0");
        output.println("Date: "+ new Date());
        output.println("Content-type: "+ contentType);
        output.println("Content-length: " + getFileLength(file));
        output.println();
        output.flush();
    }

    private void sendGET(File file, String contentType){
        // return the header + file
        output.println("HTTP/1.1 200 OK");
        output.println("Server: Java HTTP Server by ACM35 : 1.0");
        output.println("Date: "+ new Date());
        output.println("Content-type: "+ contentType);
        output.println("Content-length: " + getFileLength(file));
        output.println();
        output.flush();
        output.println(getData(file));
        output.flush();
    }

    private void sendGETImage(File file, String contentType){
        output.println("HTTP/1.1 200 OK");
        output.println("Server: Java HTTP Server by ACM35 : 1.0");
        output.println("Date: "+ new Date());
        output.println("Content-type: "+ contentType);
        output.println("Content-length: " + getFileLength(file));
        output.println();
        output.flush();


        try {
            byte[] buffer = new byte[getFileLength(file)];
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));

            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                buffOut.write(buffer, 0, bytesRead);
            }
            output.println(getData(file));

        } catch (IOException e){
            System.out.println("SEND GET IMAGE EXCEPTION: " + e.getMessage());
        }
        output.flush();
    }

}
