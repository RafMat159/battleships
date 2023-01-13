package pk.pwjj.klient;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import pk.pwjj.klient.Board.Cell;

public class BattleshipMain extends Application {

    private boolean running = false;
    private boolean start = false;
    private Board enemyBoard, playerBoard;

    private int shipsToPlace = 5;

    private boolean enemyTurn = false;


    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private Boolean waitForEnemyMove;
    private String globalMessage;
    private final Lock lock = new ReentrantLock();

    private Parent createContent() {
        BorderPane root = new BorderPane();
        root.setPrefSize(600, 800);

        root.setRight(new Text("RIGHT SIDEBAR - CONTROLS"));

        enemyBoard = new Board(true, event -> {
            if (!running || enemyTurn)
                return;

            Board.Cell cell = (Board.Cell) event.getSource();

            // tu sprawdza czy raz już ktoś szczzelił
            if (cell.wasShot) {
                return;
            }

            //wysyła wiadomość
            if(start) {
                send(String.valueOf(cell.x) + String.valueOf(cell.y));
                enemyTurn = true;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    //czeka na odpowiedź
//                    boolean respRead = false;
//                    while (!respRead) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        }
                        catch (InterruptedException e){
                            return;
                        }
                        lock.lock();
                        String resp;
                        try {
                            resp = globalMessage;
                            System.out.println("Taki event dostaje response jak odczyta po blokadzie: " + resp);
                            if (resp == null){
                                return;
                            }

                            cell.setFill(Color.BLACK);

                            // jeśli trafione to można strzelać po raz kolejny i zamalować kafelek
                            if (resp.equals("hit") || resp.equals("lose") || resp.equals("win")) {
                                enemyTurn = false;
                                cell.setFill(Color.RED);
                                cell.setDisable(true);
                                if(resp.equals("lose")){

                                } else if(resp.equals("win")){

                                }
                                //tutaj zamalować odpowiedni kafelek
                            }

//                            if (resp.equals("lose")) {
//                                System.out.println("YOU WIN");
//                                //start = false;
//                                System.exit(0);
//                            }
//                            respRead = true;
                            globalMessage = null;

                            if (resp.equals("miss")) {
                                enemyTurn = true;
                                send("end");
                                cell.setDisable(true);
//                                break;
                            }

                        } finally {
                            lock.unlock();
                            System.out.println("CATCH");
                        }

//                    }
                }
            }).start();

        });

        playerBoard = new Board(false, event -> {
            if (running)
                return;

            Board.Cell cell = (Board.Cell) event.getSource();
            if (playerBoard.placeShip(new Ship(shipsToPlace, event.getButton() == MouseButton.PRIMARY), cell.x, cell.y)) {
                if (--shipsToPlace == 0) {
                    running = true;
                    send("Your opponent has finished placing ships");
                }
            }
        });

        VBox vbox = new VBox(50, enemyBoard, playerBoard);
        vbox.setAlignment(Pos.CENTER);

        root.setCenter(vbox);

        return root;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your name");
        String username = scanner.nextLine();
        Socket socket = new Socket("localhost", 1234);

        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            this.gameController = new GameController();
//            this.battleshipMain = new BattleshipMain(this.bufferedWriter);
            this.username = username;
            this.waitForEnemyMove = false;
            send(username);
            if((bufferedReader.readLine().equals("first"))) {
                System.out.println("Starts First");
                enemyTurn = false;
                this.waitForEnemyMove = false;
            } else{
                System.out.println("Starts second");
                enemyTurn = true;
                this.waitForEnemyMove = true;
            }
        } catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
        listenForMessage();

        Scene scene = new Scene(createContent());
        primaryStage.setTitle(username);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args){
        launch();
    }

    private void send(String msg){
        try{
            System.out.println("SEND MESSAGE: "+msg);
            bufferedWriter.flush();
            bufferedWriter.write(msg);
            bufferedWriter.newLine();
            bufferedWriter.flush();
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
                    try{
                        msgFromGroupChat = bufferedReader.readLine();
                        System.out.println("Co dostał: "+msgFromGroupChat);

                        if(msgFromGroupChat.equals("Your opponent has finished placing ships"))
                            start = true;

                        if(msgFromGroupChat.length()==2 && start) {
                            Cell cell = playerBoard.getCell(msgFromGroupChat.charAt(0)-'0', msgFromGroupChat.charAt(1)-'0');
                            System.out.println("Sprawdz pole: "+ cell.x+ cell.y);
                            if (cell.shoot()){
                                send("hit");
                                if (playerBoard.ships == 0) {
                                    send("win");
                                }
                            } else{
                                send("miss");
                                enemyTurn = false;
                            }
                        }
                        else if (msgFromGroupChat.equals("end") && start) {
                            enemyTurn = false;

                        }
                        else if ((msgFromGroupChat.equals("hit") || msgFromGroupChat.equals("miss")) && start) {
                                lock.lock();
                                try {
                                    globalMessage = msgFromGroupChat;
                                    System.out.println("USTAWIA CZYU NIE"+globalMessage);
                                } finally {
                                    lock.unlock();
                                }
                        }
                        if(msgFromGroupChat.equals("lose") && start){
                            System.out.println("YOU LOOOOOOSE");

                            start = false;
                        }
                        if(msgFromGroupChat.equals("win") && start){
                            System.out.println("YOU WIN");
                            send("lose");
                            start = false;
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

}