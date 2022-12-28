package pk.pwjj.klient;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private GameController gameController;
    private String username;
    private Boolean waitForEnemyMove;


    public Client(Socket socket, String username){
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.gameController = new GameController();
//            this.battleshipMain = new BattleshipMain(this.bufferedWriter);
            this.username = username;
            this.waitForEnemyMove = false;
            send(username);
            if((bufferedReader.readLine().equals("first"))) {
                System.out.println("Starts First");
                this.waitForEnemyMove = false;
            } else{
                System.out.println("Starts second");
                this.waitForEnemyMove = true;
            }
        } catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }


    private void send(String msg) throws IOException{
        try{
            System.out.println("SEND MESSAGE: "+msg);
            bufferedWriter.write(msg);
            bufferedWriter.newLine();
            bufferedWriter.flush();
         } catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void printWaitInfo(){
        gameController.printOwnBoard();
        gameController.printEnemyBoard();
        System.out.println("Waiting for opponent...");
    }

    public void sendMessage(){
        try{
//            bufferedWriter.write(username);
//            bufferedWriter.newLine();
//            bufferedWriter.flush();

//            printWaitInfo();

            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()){
//                while (this.waitForEnemyMove){
//                    try {
//                        TimeUnit.MILLISECONDS.sleep(1000);
//                    }
//                    catch (InterruptedException e){
//                        break;
//                    }
//                }
                System.out.println("Your move: ");
                String messageToSend = scanner.nextLine();
                this.send(messageToSend);
//                this.waitForEnemyMove=true;
            }
        } catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void listenForMessage(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;
                while (socket.isConnected()){
                    String msgToEnemy = "";
                    try{
                        msgFromGroupChat = bufferedReader.readLine();
                        System.out.print("Recieved: ");
                        send("end");

                        if(msgFromGroupChat.length()==2) {
                            msgToEnemy = gameController.checkIfHit(msgFromGroupChat);
                            if (msgFromGroupChat.equals("end")) {
//                                send(msgFromGroupChat+ " hit");
//                                send("hit");
//                                send(msgToEnemy);
//                                System.out.println("GAME LOST");
                                waitForEnemyMove = false;
                            } else {
//                                send(msgFromGroupChat + " " + msgToEnemy);
                                send(msgToEnemy);
                            }
                            waitForEnemyMove = false;
                            send("end");
//                        } else if(msgFromGroupChat.equals("end")){
//                            System.out.println("GAME WON");
                        }
                        else{
                            String[] coordsAndValue = msgFromGroupChat.split(" ");
                            if (coordsAndValue.length > 1){
                                if (coordsAndValue[1].equals("miss") || coordsAndValue[1].equals("hit"))
                                    gameController.addToEnemyBoard(coordsAndValue);
                            }
                            printWaitInfo();
                        }
                        System.out.println("RECIEVED: "+msgFromGroupChat);
                    } catch (IOException e){
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }).start();
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
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

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your name");
        String username = scanner.nextLine();
        Socket socket = new Socket("localhost", 1234);
        Client client = new Client(socket, username);
        client.listenForMessage();
        client.sendMessage();

    }


}
