import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.Socket;

public class ConnectionHandler extends Thread {

    private Socket socket;
    private String directory;
    private int helper;

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
                //if(helper == 0) {
                    //helper++;

                    //Parse request
                    String[] parsedRequest = request.split(" ");

                    System.out.println(ThreadColor.ANSI_GREEN+"String 0 = " + parsedRequest[0]);
                    String requestType = parsedRequest[0];
                    System.out.println(ThreadColor.ANSI_GREEN+"String 1 = " + parsedRequest[1]);
                    String requestDirectory = directory + parsedRequest[1];
                    if(requestType.equals("HEAD")){
                        // get the header
                        getHeader(requestDirectory);
                        // return the header

                    }
                    if(requestType.equals("GET")){ // check if it is GET
                        // return the header + file
                        String response = getWhole(requestDirectory);
                        System.out.println(ThreadColor.ANSI_PURPLE+response);
                        output.println(response);
                        break;//if so then break
                    }
                    //output.println("Echo: " + request); //Echo back using printWriter

                //}

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

    private String getWhole(String requestedDirectory){
        File requestedFile = new File(requestedDirectory);
        long fileLength = requestedFile.length();

        System.out.println(requestedDirectory);
        return "404";
    }

}
