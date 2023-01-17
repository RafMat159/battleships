package pk.pwjj.klient;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
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

    private Stage primaryStage;

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
            if (start) {
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
                    } catch (InterruptedException e) {
                        return;
                    }
                    lock.lock();
                    String resp;
                    try {
                        resp = globalMessage;
                        System.out.println("Taki event dostaje response jak odczyta po blokadzie: " + resp);
                        if (resp == null) {
                            return;
                        }

                        cell.setFill(Color.BLACK);

                        // jeśli trafione to można strzelać po raz kolejny i zamalować kafelek
                        if (resp.equals("hit")) {
                            enemyTurn = false;
                            cell.setFill(Color.RED);
                            cell.setDisable(true);
                            //tutaj zamalować odpowiedni kafelek
                        }

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

    private void newConnection() throws Exception{
        Socket socket = new Socket("localhost", 1234);

        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            this.gameController = new GameController();
//            this.battleshipMain = new BattleshipMain(this.bufferedWriter);
            this.waitForEnemyMove = false;
            send(this.username);
            //readLine - operacja blokująca, zawsze czekać będzie na odpowiedź od serwera
            if ((bufferedReader.readLine().equals("first"))) {
                System.out.println("Starts First");
                enemyTurn = false;
                this.waitForEnemyMove = false;
            } else {
                System.out.println("Starts second");
                enemyTurn = true;
                this.waitForEnemyMove = true;
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }
    public void init(Stage stage) {
        Platform.runLater(()-> {
            try {
               // this.primaryStage = primaryStage;

                StackPane root = new StackPane();

                VBox vBox = new VBox();

                vBox.setSpacing(8);
                vBox.setPadding(new Insets(10, 10, 10, 10));

                TextField login= new TextField();
                PasswordField password=new PasswordField();
                Button button=new Button("LOGIN");
                button.setOnAction(actionEvent-> {
                    System.out.println(login.getText()+" "+password.getText());
                    try {
                        newConnection();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    newGame();
                    listenForMessage();
                });

                vBox.getChildren().addAll(
                        new Label("Your Username"),
                        login,
                        new Label("Your Password"),
                        password,
                        button);
                root.getChildren().addAll(vBox);

                Scene scene = new Scene(root, 400, 600);

                stage.setScene(scene);
                stage.show();
            } catch (Exception e) {
                System.out.println("BŁĄD jakiś");
                e.printStackTrace();
            }
        });
    }
    private void newGame(){
        Scene scene = new Scene(createContent());

        this.primaryStage.setScene(scene);

        this.primaryStage.show();
    }
    @Override
    public void start(Stage primaryStage) throws Exception {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your name");
        this.username = scanner.nextLine();
        this.primaryStage=primaryStage;

        Platform.runLater(()->{
            try {
               init(this.primaryStage);

               // startNewGame = false;
            } catch (Exception e) {
                System.out.println("BŁĄD jakiś");
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        launch();
    }

    private void send(String msg) {
        try {
            System.out.println("SEND MESSAGE: " + msg);
            bufferedWriter.flush();
            bufferedWriter.write(msg);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }


    public void checkHit(String cords) {
        Cell cell = playerBoard.getCell(cords.charAt(0) - '0', cords.charAt(1) - '0');
        System.out.println("Sprawdz pole: " + cell.x + cell.y);
        if (cell.shoot()) {
            send("hit");
            if (playerBoard.ships == 0) {
                send("win");
                System.out.println("GAME LOST");
                enemyTurn = true;
            }
        } else {
            send("miss");
            enemyTurn = false;
        }
    }

    public void listenForMessage(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;

                while (socket.isConnected()) {
                    try {
                        msgFromGroupChat = bufferedReader.readLine();
                        System.out.println("Co dostał: " + msgFromGroupChat);

                        if (msgFromGroupChat.equals("Your opponent has finished placing ships"))
                            start = true;

                        if (start) {
                            if (msgFromGroupChat.length() == 2) {
                                checkHit(msgFromGroupChat);
                            } else{
                                switch (msgFromGroupChat){
                                    case "end":
                                        enemyTurn = false;
                                        break;
                                    case "hit": case "miss":
                                        lock.lock();
                                        try {
                                            globalMessage = msgFromGroupChat;
                                            System.out.println("USTAWIA CZYU NIE" + globalMessage);
                                        } finally {
                                            lock.unlock();
                                        }
                                        break;
                                    case "win":
                                        if (msgFromGroupChat.equals("win")) {
                                            System.out.println("YOU WIN");
                                            closeEverything(socket, bufferedReader, bufferedWriter);
                                            start = false;
                                            System.out.println("START NEW GAME?");
                                            Scanner scanner = new Scanner(System.in);
                                            String choice = scanner.next();
                                            System.out.println(choice);
                                            if(choice=="yes"){
                                                try {
                                                    newConnection();
                                                    newGame();
                                                } catch (Exception e) {
                                                    System.out.println("COULDN'T CREATE NEW GAME. EXITING...");
                                                }
                                            }
                                            break;
                                        }
                                    default:
                                        System.out.println("COMMAND NOT RECOGNIZED");
                                }
                            }
                        }

                        System.out.println("RECIEVED: " + msgFromGroupChat);
                    } catch (IOException e) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }).start();
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}