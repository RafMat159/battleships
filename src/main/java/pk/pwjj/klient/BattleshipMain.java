package pk.pwjj.klient;

import java.io.*;
import java.net.Socket;
import java.util.Optional;
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

public class BattleshipMain extends Application {

    private boolean running = false;
    private boolean start = false;
    private Board enemyBoard, playerBoard;

    private int shipsToPlace = 5;

    private boolean enemyTurn = false;

    private Boolean mayPlaceShips = false;
    private String winStatus;

    private Stage primaryStage;

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private String globalMessage;
    private final Lock lock = new ReentrantLock();
    private Thread listenerThread;
    private BorderPane root;
    private int width = 600;
    private int height = 800;
    private GameController gameController=new GameController();
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
                //                winStatus = "GAME LOST";
                //endGameScreen();
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
            System.out.println("USERNAME: "+this.username);
            send(this.username);
            //readLine - operacja blokująca, zawsze czekać będzie na odpowiedź od serwera
//            if ((bufferedReader.readLine().equals("first"))) {
//                System.out.println("Starts First");
//                enemyTurn = false;
//            } else {
//                System.out.println("Starts second");
//                enemyTurn = true;
//            }
        } catch (IOException e) {
            closeConnection();
        }
        listenForMessage();
    }
    public void init(Stage stage) {
        Platform.runLater(()-> {
            try {
               // this.primaryStage = primaryStage;
                LoginController loginController=new LoginController();

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
                        int resp= loginController.login(login.getText(),password.getText());
                        if(resp==0){
                            Alert a = new Alert(Alert.AlertType.NONE,"Wciśnij ok aby rozpocząć grę",ButtonType.OK);
                            a.setTitle("Utworzono konto");
                            a.showAndWait();
                        }
                        if(resp==1||resp==0)
                        {
                            this.username = login.getText();
                            try {
                                newConnection();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            buildScene();
                            listenForMessage();
                        }
                        else if(resp==-1){
                            Alert a = new Alert(Alert.AlertType.ERROR);
                            a.setHeaderText("Wpisano złe hasło!");
                            a.setTitle("Odmowa dostępu!");
                            a.showAndWait();
                        }
                    }
                    else{
                        Alert a = new Alert(Alert.AlertType.WARNING);
                        a.setHeaderText("Nie uzupełniono wszystkich pól!");
                        a.setTitle("Błąd!");
                        a.showAndWait();
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
            buildScene();
        }

    }

    public void buildScene(){
        Platform.runLater(()-> {
            Scene scene = new Scene(createContent());
            this.primaryStage.setTitle(username);
            this.primaryStage.setScene(scene);
            this.primaryStage.setResizable(false);
            this.primaryStage.show();
        });
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

    @Override
    public void stop(){
        System.out.println("App exit");
        closeConnection();
        System.exit(0);
//        Platform.exit();
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
        }
    }


    public void checkHit(String cords) {
        Cell cell = playerBoard.getCell(cords.charAt(0) - '0', cords.charAt(1) - '0');
        System.out.println("Sprawdz pole: " + cell.x + cell.y);
        if (cell.shoot()) {
            send("hit");
            if (playerBoard.ships == 0) {
               gameController.updateRanking(this.username,"lose");
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

    public void restartGame(){
        System.out.println("Restarting game");
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

    public void endGameScreen(){
        //enemyTurn = true;

        send("end game");
        Platform.runLater(()-> {
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
//                popupStage.getX();
                resume.setOnAction(event -> {
                    root.setEffect(null);
                    popupStage.hide();
                    restartGame();
                });

                end.setOnAction(event -> {
//                    System.exit(0);
                    // chyba to powinno być żeby stop triggerować
                    Platform.exit();
                });

                popupStage.show();
                } catch (Exception e) {
                System.out.println("BŁĄD jakiś");
                e.printStackTrace();
            }
        });
    }
    public void listenForMessage(){
        listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;

                while (socket.isConnected()) {
                    try {
                        msgFromGroupChat = bufferedReader.readLine();
                        System.out.println("Co dostał: " + msgFromGroupChat);

                        // tu wpada w pętlę po naciśnięciu new game przez jednego z graczy
                        // wysyłany jest komunikat left, dla tamtego usera jest tworzony nowy pokój i od razu dla tego
                        // pasuje jakoś rozrózniać te komunikaty
                        if (msgFromGroupChat == null || msgFromGroupChat.equals("left")) {
                            restartGame();
                            continue;
                        }

                        if(!start) {
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
                                        gameController.updateRanking(username,"win");
                                        System.out.println("YOU WIN");
                                        winStatus = "GAME WON";
                                        endGameScreen();
                                        break;

                                    default:
                                        System.out.println("COMMAND NOT RECOGNIZED");
                                }
                            }
                        }

                        System.out.println("RECIEVED: " + msgFromGroupChat);
                    } catch (IOException e) {
                        closeConnection();
                    }
                }
            }
        });
        listenerThread.start();
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