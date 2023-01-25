package pk.pwjj.klient;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/***
 * ClientHandler is responsible for communication client-server
 */
public class ClientHandler implements Runnable{

    /** all available boards */
    public static HashMap<Integer, ArrayList<ClientHandler>> clientMap = new HashMap<>();
    /** map of all available boards*/
    public static HashMap<Integer, Boolean> available = new HashMap<>();
    /** all active users */
    public static Set<String> users = new HashSet<>();
    private Socket socket;
    /** number of table*/
    private int table;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    /** username of client*/
    private String clientUsername;
    /** indicates if client can start game first*/
    private Boolean startsFirst=false;

    /**
     * Function that chooses first available table for certain client.
     * @return number of table
     */
    public int getClosestAvailable(){
        int maxKey = 0;
        for (Integer key : available.keySet()){
            maxKey = key > maxKey ? key : maxKey;
            if(available.get(key))
                return key;
        }
        available.put(maxKey+1, true);
        return maxKey+1;
    }

    /**
     * Function responsible for adding first client to table.
     * @throws IOException
     */
    public void addToTable() throws IOException{
        table = getClosestAvailable();
        ArrayList<ClientHandler> arr = clientMap.get(table);
        if (arr != null) {
            arr.add(this);
            System.out.println(arr.size());
            if (arr.size() == 2) {
                available.put(table, false);
                sendToOpponent("first");
                sendToClient("second");
            }
        }
        else{
            arr = new ArrayList<>();
            arr.add(this);
        }
        clientMap.put(table, arr);
        System.out.println("User: "+clientUsername+", dodano do stolu: "+table);
    }

    /**
     * Initiates ClientHandler class.
     * @param socket of client
     */
    public ClientHandler(Socket socket){

        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();
            if(users.contains(clientUsername)){
                sendToClient("login error");
            }
            else {
                users.add(this.clientUsername);
                sendToClient("login successful");
                this.addToTable();
                sendToOpponent("SERVER:" + clientUsername + " joined chat");
            }
        } catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    /**
     * Function that handles removing client from table and adding to new game.
     */
    public void sendToClient(String msg){
        try {
            System.out.println("SEND MESSAGE: " + msg);
            bufferedWriter.flush();
            bufferedWriter.write(msg);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        } catch (NullPointerException e) {
            System.out.println("Message is null, cannot send");
        }
    }

    public void newGame(){
        removeFromTable();
        try{
            this.addToTable();
        } catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    /**
     * Handles handles chat and game commands of certain client.
     */
    @Override
    public void run(){
        String messageFromClient;
        while (socket.isConnected()){
            try{
                messageFromClient = bufferedReader.readLine();
                System.out.println("User: "+clientUsername+": "+messageFromClient);

                if(messageFromClient.startsWith("communication:")){                                         // chat communication
                    String strippedMessage = messageFromClient.substring(14);
                    sendToOpponent("communication:"+clientUsername+": "+strippedMessage);
                }
                else {
                    switch (messageFromClient) {                                                            // commands
                        case "new game":
                            this.newGame();
                            break;
                        case "end game":
                            closeEverything(socket, bufferedReader, bufferedWriter);
                            break;
                        default:
                            sendToOpponent(messageFromClient);
                    }
                }
            } catch (Exception e){
                closeEverything(socket,  bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    /**
     * Sends message to another client at table.
     * @param messageToSend
     */
    public void sendToOpponent(String messageToSend){
        ArrayList<ClientHandler> arr = clientMap.get(table);
        for (ClientHandler clientHandler : arr){
            try {
                if(arr != null && !clientHandler.clientUsername.equals((this.clientUsername))){
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();

                }
            } catch (IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    /**
     * Removes client from table.
     */
    public void removeFromTable(){
        try {
            var removingSuccessfull = clientMap.get(table).remove(this);
            if (removingSuccessfull) {
                System.out.println("User: " + clientUsername + ", usunięto ze stołu: " + table);
                if (clientMap.get(table).size() == 0) {
                    System.out.println("Table " + table + " empty");
                    clientMap.put(table, null);
                } else
                    sendToOpponent("left");
                available.put(table, true);
                table = 0;
            }
        } catch (NullPointerException e){
            System.out.println("Table "+table+" already empty");
        }
    }

    /**
     * Closes all connections of client.
     * @param socket
     * @param bufferedReader
     * @param bufferedWriter
     */
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        if (table != 0)
            removeFromTable();

        if(users.contains(this.clientUsername))
            users.remove(this.clientUsername);

        try{
            if(bufferedReader !=null){
                bufferedReader.close();
            }
            if(bufferedWriter !=null){
                bufferedWriter.close();
            }
            if(socket !=null){
                socket.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

}
