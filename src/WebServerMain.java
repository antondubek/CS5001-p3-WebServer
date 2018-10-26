public class WebServerMain {

    public static void main(String[] args) {

        // Check the arguments
        if (args.length == 2) {
            String directory = args[0];
            int port = Integer.parseInt(args[1]);

            Server server = new Server(directory, port, 1);
            new Thread(server).start();

        } else {
            System.out.println("Usage: java WebServerMain <document_root> <port>");
        }

    }
}
