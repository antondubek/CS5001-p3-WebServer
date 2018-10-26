public class WebServerMain {

    public static void main(String[] args) {

        // Check the arguments
        if(args.length != 2){
            System.out.println("Usage: java WebServerMain <document_root> <port>");
        }

        String directory = args[0];
        int port = Integer.parseInt(args[1]);

        new Server(directory, port);

    }
}
