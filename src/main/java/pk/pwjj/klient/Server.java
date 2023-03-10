package pk.pwjj.klient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Game server.
 */
public class Server {

    private ServerSocket serverSocket;

    /**
     * Initiation of Server.
     *
     * @param serverSocket socket that handles Server communication
     */
    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /**
     * Starting Server.
     */
    public void startServer() {

        try {

            while (!serverSocket.isClosed()) {

                Socket socket = serverSocket.accept();
                System.out.println("A new client has connected!");
                ClientHandler clientHandler = new ClientHandler(socket);

                Thread thread = new Thread(clientHandler);
                thread.start();


            }
        } catch (IOException e) {
            closeServerSocket();
        }


    }

    /**
     * Closes socket of Server.
     */
    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Hello world!");
        ServerSocket serverSocket = new ServerSocket(1234);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}