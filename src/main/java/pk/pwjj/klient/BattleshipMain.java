package pk.pwjj.klient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import pk.pwjj.controller.GameController;
import pk.pwjj.controller.LoginController;
import pk.pwjj.klient.Board.Cell;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BattleshipMain extends Application {

    private boolean running = false;
    private boolean start = false;
    private boolean enemyTurn = false;
    private Boolean mayPlaceShips = false;
    private String winStatus = null;
    private Board enemyBoard, playerBoard;
    private int shipsToPlace = 5;


    private Stage primaryStage;
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private String globalMessage;
    private final Lock lock = new ReentrantLock();
    private BorderPane root;
    private final int width = 600;
    private final int height = 800;

    private Parent createContent() {
        root = new BorderPane();
        root.setPrefSize(width, height);

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


                }
            }).start();

        });

        shipsToPlace = 5;

        playerBoard = new Board(false, event -> {
            if (running)
                return;

            Board.Cell cell = (Board.Cell) event.getSource();
            if (mayPlaceShips && playerBoard.placeShip(new Ship(shipsToPlace, event.getButton() == MouseButton.PRIMARY), cell.x, cell.y)) {
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

    public void newConnection() throws Exception {
        Socket socket = new Socket("localhost", 1234);

        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("USERNAME: " + this.username);
            send(this.username);
        } catch (IOException e) {
            closeConnection();
        }
        listenForMessage();
        chatWriter();
    }

    public void init(Stage stage) {
        Platform.runLater(() -> {
            try {
                StackPane root = new StackPane();

                VBox vBox = new VBox();

                vBox.setSpacing(8);
                vBox.setPadding(new Insets(10, 10, 10, 10));

                TextField login = new TextField();
                PasswordField password = new PasswordField();
                Button button = new Button("LOGIN");

                button.setOnAction(actionEvent -> {
                    System.out.println(login.getText() + " " + password.getText());
                    if (login.getText().length() != 0 && password.getText().length() != 0) {

                        int resp = LoginController.getInstance().login(login.getText(), password.getText());
                        Alert a;

                        switch (resp) {

                            case 0:
                                a = new Alert(Alert.AlertType.NONE, "Wciśnij ok aby rozpocząć grę", ButtonType.OK);
                                a.setTitle("Utworzono konto");
                                a.showAndWait();

                            case 1:
                                this.username = login.getText();

                                try {
                                    newConnection();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }

                                buildScene();
//                                to je chyba niepotrzebne
//                                listenForMessage();
                                break;

                            case -1:
                                a = new Alert(Alert.AlertType.ERROR);
                                a.setHeaderText("Wpisano złe hasło!");
                                a.setTitle("Odmowa dostępu!");
                                a.showAndWait();
                                break;

                            default:
                                a = new Alert(Alert.AlertType.WARNING);
                                a.setHeaderText("Nie uzupełniono wszystkich pól!");
                                a.setTitle("Błąd!");
                                a.showAndWait();
                                break;
                        }
                    }
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

    public void buildScene() {
        Platform.runLater(() -> {
            Scene scene = new Scene(createContent());
            this.primaryStage.setTitle(username);
            this.primaryStage.setScene(scene);
            this.primaryStage.setResizable(false);
            this.primaryStage.show();
        });
    }

    @Override
    public void start(Stage primaryStage){
        this.primaryStage = primaryStage;
        Platform.runLater(() -> {
            try {
                init(this.primaryStage);
            } catch (Exception e) {
                System.out.println("BŁĄD jakiś");
                e.printStackTrace();
            }
        });

    }

    @Override
    public void stop() {
        System.out.println("App exit");
        send("end game");
        closeConnection();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("In shutdown hook");
            }
        }, "Shutdown-thread"));
    }

    private void send(String msg) {
        try {
            System.out.println("SEND MESSAGE: " + msg);
            bufferedWriter.flush();
            bufferedWriter.write(msg);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeConnection();
        } catch (NullPointerException e){
        }
    }


    public void checkHit(String cords) {
        Cell cell = playerBoard.getCell(cords.charAt(0) - '0', cords.charAt(1) - '0');
        System.out.println("Sprawdz pole: " + cell.x + cell.y);
        if (cell.shoot()) {

            send("hit");
            if (playerBoard.ships == 0) {
                GameController.getInstance().updateRanking(this.username, "lose");
                send("win");
                winStatus = "GAME LOST";
                System.out.println("GAME LOST");
                enemyTurn = true;
                endGameScreen();
            }

        } else {
            send("miss");
            enemyTurn = false;
        }
    }

    public void restartGame() {
        System.out.println("Restarting game");

        // game is neither won nor lost at this stage
        winStatus = null;
        // block placing ships until new player arrives
        mayPlaceShips = false;
        // wait to start game
        start = false;
        // set to false, so the ships can be placed again
        running = false;
        // set message to get new table
        send("new game");
        // build new board
        buildScene();
    }

    public void endGameScreen() {
        //enemyTurn = true;

        Platform.runLater(() -> {
            try {

                root.setEffect(new GaussianBlur());

                VBox pauseRoot = new VBox(5);
                pauseRoot.getChildren().add(new Label(winStatus));
                pauseRoot.getChildren().add(new Label("Play next game or exit"));
                pauseRoot.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8);");
                pauseRoot.setAlignment(Pos.CENTER);
                pauseRoot.setPadding(new Insets(20));

                HBox buttonRoot = new HBox(20);
                pauseRoot.setAlignment(Pos.CENTER);
                pauseRoot.setPrefWidth(180);
                pauseRoot.setPrefHeight(100);
                pauseRoot.setPadding(new Insets(20));
                pauseRoot.getChildren().add(buttonRoot);

                Button resume = new Button("Play");
                resume.setMinWidth(60);

                Button end = new Button("Exit");
                end.setMinWidth(60);

                buttonRoot.getChildren().add(end);
                buttonRoot.getChildren().add(resume);
                Stage popupStage = new Stage(StageStyle.TRANSPARENT);

                popupStage.initOwner(primaryStage);
                popupStage.initModality(Modality.APPLICATION_MODAL);
                popupStage.setScene(new Scene(pauseRoot, Color.TRANSPARENT));
                System.out.println(pauseRoot.getWidth());
                popupStage.setX(primaryStage.getX() + (primaryStage.getWidth() / 2) - pauseRoot.getPrefWidth() / 2);
                popupStage.setY(primaryStage.getY() + (primaryStage.getHeight() / 2) - pauseRoot.getPrefHeight() / 2);

                resume.setOnAction(event -> {
                    root.setEffect(null);
                    popupStage.hide();
                    restartGame();
                });

                end.setOnAction(event -> {
                    Platform.exit();
                });

                popupStage.show();
            } catch (Exception e) {
                System.out.println("BŁĄD jakiś");
                e.printStackTrace();
            }
        });
    }

    // send chat message with specific prefix
    public void chatWriter(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String chatMsgToSend;
                Scanner scanner = new Scanner(System.in);
                while (socket.isConnected()){
                    chatMsgToSend = scanner.nextLine();
                    send("communication:"+chatMsgToSend);
                }
            }
        }).start();
    }

    public void listenForMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;

                while (socket.isConnected()) {
                    try {
                        msgFromGroupChat = bufferedReader.readLine();
                        System.out.println("Co dostał: " + msgFromGroupChat);

                        // if someone left, and the game has no result (someone left during game) and was started then restart game
                        if (msgFromGroupChat == null || (msgFromGroupChat.equals("left") && winStatus == null && start)) {
                            restartGame();
                            continue;
                        }

                        // messages to check before game starts
                        if (!start) {
                            switch (msgFromGroupChat) {
                                case "first":
                                    System.out.println("Starts First");
                                    enemyTurn = false;
                                    // placing ships blocked until two players at the table
                                    mayPlaceShips = true;
                                    break;

                                case "second":
                                    System.out.println("Starts second");
                                    enemyTurn = true;
                                    // placing ships blocked until two players at the table
                                    mayPlaceShips = true;
                                    break;

                                case "Your opponent has finished placing ships":
                                    start = true;
                                    break;
                            }
                        }

                        // messages to check when game is running
                        if (start) {

                            if (msgFromGroupChat.length() == 2) {                                   // board coordinates
                                checkHit(msgFromGroupChat);
                            } else if (msgFromGroupChat.startsWith("communication:")) {             // chat
                                System.out.println(msgFromGroupChat.substring(14));
                            } else {                                                                // commands
                                switch (msgFromGroupChat) {

                                    case "end":
                                        enemyTurn = false;
                                        break;

                                    case "hit":
                                    case "miss":
                                        lock.lock();
                                        try {
                                            globalMessage = msgFromGroupChat;
                                            System.out.println("USTAWIA CZYU NIE" + globalMessage);
                                        } finally {
                                            lock.unlock();
                                        }
                                        break;

                                    case "win":
                                        GameController.getInstance().updateRanking(username, "win");
                                        System.out.println("YOU WIN");
                                        winStatus = "GAME WON";
                                        endGameScreen();
                                        break;

                                    default:
                                        System.out.println("COMMAND NOT RECOGNIZED");
                                }
                            }
                        }
                    } catch (IOException e) {
                        closeConnection();
                    }
                }
            }
        }).start();
    }

    public void closeConnection() {
        try {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}