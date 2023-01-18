package pk.pwjj.klient;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class ClientHandler implements Runnable{

//    public static ArrayList<pk.pwjj.klient.ClientHandler> clientHandlers = new ArrayList<>();
    public static HashMap<Integer, ArrayList<ClientHandler>> clientMap = new HashMap<>();
    public static HashMap<Integer, Boolean> available = new HashMap<>();
    private Socket socket;
    private int table;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;
    private Boolean startsFirst=false;


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

    public void addToTable() throws IOException{

        table = getClosestAvailable();
        ArrayList<ClientHandler> arr = clientMap.get(table);
        if (arr != null) {
            arr.add(this);
//            startsFirst = true;
            System.out.println(arr.size());
            if (arr.size() == 2) {
                available.put(table, false);
                broadcastMessage("first");
                this.bufferedWriter.write("second");
                this.bufferedWriter.newLine();
                this.bufferedWriter.flush();
            }
//            clientMap.put(table, arr);
        }
        else{
            arr = new ArrayList<>();
            arr.add(this);
        }
        clientMap.put(table, arr);
        System.out.println("User: "+clientUsername+", dodano do stolu: "+table);
    }

    public ClientHandler(Socket socket){

        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();
            this.addToTable();
            broadcastMessage("SERVER:"+clientUsername+" joined chat");
        } catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void newGame(){
        clientMap.get(table).remove(this);
        System.out.println("Usunięto ze stołu: "+table);
        try{
            this.addToTable();
        } catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }
    @Override
    public void run(){
        String messageFromClient;

        while (socket.isConnected()){
            try{
                messageFromClient = bufferedReader.readLine();
                if(messageFromClient.equals("new game")){
                    this.newGame();
                } else {
                    broadcastMessage(messageFromClient);
                }
            } catch (Exception e){
                closeEverything(socket,  bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    public void broadcastMessage(String messageToSend){
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

    public void removeClientHanlder(){
        clientMap.get(table).remove(this);
        System.out.println("To zapewne tu się wywala");
        if(clientMap.get(table).size() == 0){
            System.out.println("Table " + table + " empty");
            clientMap.put(table, null);
        } else
            broadcastMessage("SERVER: "+clientUsername+" has left");
        System.out.println("chyba ta");

        available.put(table, true);
//        clientHandlers.remove(this);
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        removeClientHanlder();
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
