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
import javafx.scene.text.Text;
import javafx.stage.Stage;

import pk.pwjj.klient.Board.Cell;

public class BattleshipMain extends Application {

    private boolean running = false;
    private Board enemyBoard, playerBoard;

    private int shipsToPlace = 5;

    private boolean enemyTurn = false;

    private Random random = new Random();


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
            send(String.valueOf(cell.x)+String.valueOf(cell.y));
            enemyTurn = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    //czeka na odpowiedź
                    lock.lock();
                    String resp;
                    try {
                        resp = globalMessage;
                    } finally {
                        lock.unlock();
                    }
                    System.out.println("Taki event dostaje response jak odczyta po blokadzie: "+resp);
                    // jeśli trafione to można strzelać po raz kolejny i zamalować kafelek
                    if (resp.equals("hit")) {
                        enemyTurn = false;
                        //tutaj zamalować odpowiedni kafelek
                    }

                    if (enemyBoard.ships == 0) {
                        System.out.println("YOU WIN");
                        System.exit(0);
                    }

                    if (resp.equals("miss")) {
                        enemyTurn = true;
                        send("end");
                    }

                }
            }).start();

//            if (enemyTurn)
//                enemyMove();
        });

        playerBoard = new Board(false, event -> {
            if (running)
                return;

            Board.Cell cell = (Board.Cell) event.getSource();
            if (playerBoard.placeShip(new Ship(shipsToPlace, event.getButton() == MouseButton.PRIMARY), cell.x, cell.y)) {
                if (--shipsToPlace == 0) {
                    startGame();
                }
            }
        });

        VBox vbox = new VBox(50, enemyBoard, playerBoard);
        vbox.setAlignment(Pos.CENTER);

        root.setCenter(vbox);

        return root;
    }

    private void enemyMove() {
        while (enemyTurn) {
            int x = random.nextInt(10);
            int y = random.nextInt(10);

            Cell cell = playerBoard.getCell(x, y);
            if (cell.wasShot)
                continue;

            enemyTurn = cell.shoot();

            if (playerBoard.ships == 0) {
                System.out.println("YOU LOSE");
                System.exit(0);
            }
        }
    }

    private void startGame() {
        // place enemy ships
        int type = 5;

        while (type > 0) {
            int x = random.nextInt(10);
            int y = random.nextInt(10);

            if (enemyBoard.placeShip(new Ship(type, Math.random() < 0.5), x, y)) {
                type--;
            }
        }

        running = true;
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
                this.waitForEnemyMove = false;
            } else{
                System.out.println("Starts second");
                this.waitForEnemyMove = true;
            }
        } catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
        listenForMessage();

        Scene scene = new Scene(createContent());
        primaryStage.setTitle("Battleship");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args){
        launch();
    }
//
//    public BattleshipMain(Socket socket, String username){
//        try{
//            this.socket = socket;
//            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
////            this.gameController = new GameController();
////            this.battleshipMain = new BattleshipMain(this.bufferedWriter);
//            this.username = username;
//            this.waitForEnemyMove = false;
//            send(username);
//            if((bufferedReader.readLine().equals("first"))) {
//                System.out.println("Starts First");
//                this.waitForEnemyMove = false;
//            } else{
//                System.out.println("Starts second");
//                this.waitForEnemyMove = true;
//            }
//        } catch (IOException e){
//            closeEverything(socket, bufferedReader, bufferedWriter);
//        }
//    }
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

//    public void printWaitInfo(){
//        gameController.printOwnBoard();
//        gameController.printEnemyBoard();
//        System.out.println("Waiting for opponent...");
//    }

    public void sendMessage(){
//            bufferedWriter.write(username);
//            bufferedWriter.newLine();
//            bufferedWriter.flush();

//            printWaitInfo();

        Scanner scanner = new Scanner(System.in);
        while (socket.isConnected()){
            while (this.waitForEnemyMove){
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                }
                catch (InterruptedException e){
                    break;
                }
            }
            System.out.println("Your move: ");
            String messageToSend = scanner.nextLine();
            this.send(messageToSend);
            this.waitForEnemyMove=true;
        }
    }

    public String readMessage(){
        String msg = "";
        try{
            msg = bufferedReader.readLine();
        } catch (IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
        return msg;
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
                        System.out.println("Co dostał: "+msgFromGroupChat);
                        if(msgFromGroupChat.length()==2) {
                            Cell cell = playerBoard.getCell(msgFromGroupChat.charAt(0)-'0', msgFromGroupChat.charAt(1)-'0');
                            System.out.println("Sprawdz pole: "+ cell.x+ cell.y);
                            String msg = "";
                            if (cell.shoot()){
                                send("hit");
                                msg = "hit";
                                if (playerBoard.ships == 0)
                                    send("end");
                            } else{
                                send("miss");
                                msg = "miss";
                                enemyTurn = false;
                            }

                            lock.lock();
                            try {
                                globalMessage = msg;
                            } finally {
                                lock.unlock();
                            }

//                            msgToEnemy = gameController.checkIfHit(msgFromGroupChat);
//                                send(msgFromGroupChat+ " hit");
//                                send(msgToEnemy);
//                                System.out.println("GAME LOST");
//                            } else {
//                                send(msgFromGroupChat + " " + msgToEnemy);
//                            }
//                            waitForEnemyMove = false;
//                        } else if(msgFromGroupChat.equals("end")){
//                            System.out.println("GAME WON");
                        }
                        if (msgFromGroupChat.equals("end")) {
                            enemyTurn = false;
                        }
//                        else{
//                            String[] coordsAndValue = msgFromGroupChat.split(" ");
//                            if (coordsAndValue.length > 1){
//                                if (coordsAndValue[1].equals("miss") || coordsAndValue[1].equals("hit"))
//                                    gameController.addToEnemyBoard(coordsAndValue);
//                            }
//                        }
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