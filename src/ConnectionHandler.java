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
    private File requestedFile;

    public ConnectionHandler(Socket socket, String directory){
        System.out.println(ThreadColor.ANSI_BLUE+"Connection Established, creating handler");
        this.socket = socket;
        this.directory = directory;
        this.helper = 0;
    }

    @Override
    public void run() {
        try{
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

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

                    if(new File(requestDirectory).isFile()) {
                        requestedFile = new File(requestDirectory);
                    } else {
                        // 404 message returned
                        output.println("HTTP/1.1 404 FILE NOT FOUND");
                        break;
                    }

                    if(requestType.equals("GET")){ // check if it is GET
                        // return the header + file
                        int fileLength = getFileLength(requestedFile);
                        //byte[] content = getData(requestedFile);
                        String content = getData(requestedFile);


                        output.println("HTTP/1.1 200 OK");
                        output.println("Server: Java HTTP Server by ACM35 : 1.0");
                        output.println("Date: "+ new Date());
                        output.println("Content-type: text/html");
                        output.println("Content-length: " + fileLength);
                        output.println();
                        output.flush();
                        output.println(content);
                        output.flush();
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

//    private byte[] getData(File requestedFile) throws IOException{
//        FileInputStream fileIn = null;
////        byte[] fileData = new byte[getFileLength(requestedFile)];
////
////        try{
////            fileIn = new FileInputStream(requestedFile);
////        } finally {
////            if(fileIn != null)
////                fileIn.close();
////        }
////
////        return fileData;
//    }

    private String getData(File requestedFile){
        try {
            Document doc = Jsoup.parse(requestedFile, "UTF-8");
            return doc.html();
        } catch (IOException e){
            System.out.println("Get Data Exception: " + e.getMessage());
        }
        return "";
    }

}
