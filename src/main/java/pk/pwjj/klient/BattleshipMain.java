package pk.pwjj.klient;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import javafx.stage.StageStyle;
import pk.pwjj.controller.LoginController;
import pk.pwjj.klient.Board.Cell;

public class BattleshipMain extends Application {

    private boolean running = false;
    private boolean start = false;
    private Board enemyBoard, playerBoard;

    private int shipsToPlace = 5;

    private boolean enemyTurn = false;

    private Boolean startNewGame = false;

    private Stage primaryStage;

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private Boolean waitForEnemyMove;
    private String globalMessage;
    private final Lock lock = new ReentrantLock();
    private BorderPane root;
    private int width = 600;
    private int height = 800;

    LoginController loginController = new LoginController();

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

        shipsToPlace = 5;
        playerBoard = new Board(false, event -> {
            if (running)
                return;

            Board.Cell cell = (Board.Cell) event.getSource();
            if (playerBoard.placeShip(new Ship(shipsToPlace, event.getButton() == MouseButton.PRIMARY), cell.x, cell.y)) {
                endGameScreen();
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

    public void newConnection() throws Exception{
        Socket socket = new Socket("localhost", 1234);

        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.waitForEnemyMove = false;
            System.out.println("USERNAME: "+this.username);
            send(this.username);
            //readLine - operacja blokująca, zawsze czekać będzie na odpowiedź od serwera
            if ((bufferedReader.readLine().equals("first"))) {
                System.out.println("Starts First");
                enemyTurn = false;
                this.waitForEnemyMove = false;
            } else {
                System.out.println("Starts second");
                send("second");
                enemyTurn = true;
                this.waitForEnemyMove = true;
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
        listenForMessage();
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
                    if(login.getText().length()!=0&&password.getText().length()!=0) {

                        if()
                        {
                            this.username = login.getText();
                            try {
                                newConnection();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            // do zmiany
                            // newGame();
                            buildScene();
                            listenForMessage();
                        }
                    }else{
                        Alert a = new Alert(Alert.AlertType.NONE);
                        // set alert type
                        a.setAlertType(Alert.AlertType.WARNING);
                        a.setHeaderText("Nie uzupełniono wszystkich pól!");
                        a.setTitle("Błąd!");
                        // show the dialog
                        a.show();
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

    public void askForNewGame(){

        System.out.println("START NEW GAME?");
        Scanner scanner = new Scanner(System.in);
        String choice = scanner.next();
        System.out.println(choice);
        endGameScreen();
        if(choice.equals("yes")) {
            send("new game");
            Platform.runLater(()->{
                try {
                    running = false;
                    buildScene();
                } catch (Exception e) {
                    System.out.println("BŁĄD jakiś");
                    e.printStackTrace();
                }
            });
        }

    }

    public void buildScene(){
        Scene scene = new Scene(createContent());
        this.primaryStage.setTitle(username);
        this.primaryStage.setScene(scene);
        this.primaryStage.setResizable(false);
        this.primaryStage.show();

    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
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
                askForNewGame();
            }
        } else {
            send("miss");
            enemyTurn = false;
        }
    }

    public void endGameScreen(){
        //enemyTurn = true;

        Platform.runLater(()-> {
            try {
                //postion
                double CENTER_ON_SCREEN_X_FRACTION = 1.0f / 2;
                double CENTER_ON_SCREEN_Y_FRACTION = 1.0f / 3;

                Screen screen = Screen.getPrimary();
                Rectangle2D bounds = screen.getVisualBounds();
              //  System.out.println(bounds.getWidth());
               // System.out.println(primaryStage.getWidth());
//                double centerX = bounds.getMinX() +(bounds.getWidth() - root.getWidth())*CENTER_ON_SCREEN_X_FRACTION;// bounds.getMinX() +
//                double centerY = bounds.getMinY() +(bounds.getHeight() -  root.getHeight())*CENTER_ON_SCREEN_Y_FRACTION;// bounds.getMinY() +

                root.setEffect(new GaussianBlur());

                VBox pauseRoot = new VBox(5);
                pauseRoot.getChildren().add(new Label("Paused"));
                pauseRoot.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8);");
                pauseRoot.setAlignment(Pos.CENTER);
                pauseRoot.setPadding(new Insets(20));

                double centerX = root.getLayoutX();
                double centerY = root.getLayoutY();

//                System.out.println();

                Button resume = new Button("Resume");
                pauseRoot.getChildren().add(resume);
                pauseRoot.setLayoutX(centerX);
                pauseRoot.setLayoutY(centerY);

                Stage popupStage = new Stage(StageStyle.TRANSPARENT);
                popupStage.initOwner(primaryStage);
                popupStage.initModality(Modality.APPLICATION_MODAL);
                popupStage.setScene(new Scene(pauseRoot, Color.TRANSPARENT));
                popupStage.setX(primaryStage.getX() + primaryStage.getWidth() / 2 - pauseRoot.getWidth());
                popupStage.setY(primaryStage.getY() + primaryStage.getHeight() / 2 - pauseRoot.getHeight());
//                popupStage.getX();
                resume.setOnAction(event -> {
//                    root.setEffect(null);
                    // animation.play();
//                    popupStage.hide();
                    int result = loginController.login("username", "password");
                    System.out.println("Request result: "+result);
                });
                popupStage.show();
                System.out.println(popupStage.getX());
            } catch (Exception e) {
                System.out.println("BŁĄD jakiś");
                e.printStackTrace();
            }
        });
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
                                            start = false;
                                            askForNewGame();
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