package pk.pwjj.klient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import pk.pwjj.DTO.UserRankingDTO;
import pk.pwjj.HibernateUtil;
import pk.pwjj.controller.GameController;
import pk.pwjj.controller.LoginController;
import pk.pwjj.entity.User;
import pk.pwjj.klient.Board.Cell;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.List;
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

    TextArea displayAllMessages = new TextArea();;
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private String globalMessage;
    private final Lock lock = new ReentrantLock();
    private BorderPane root;
    private final int width = 800;
    private final int height = 800;

    private Parent createContent() {
        root = new BorderPane();
        root.setPrefSize(width, height);

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
                        if (resp.equals("hit") || resp.equals("sunk")) {
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

        GridPane chatPane = createChat();

        HBox hbox = new HBox(50, vbox, chatPane);
        hbox.setPadding(new Insets(50));

        root.setCenter(hbox);

        return root;
    }

    public void addMessage(String msg) {
        displayAllMessages.appendText(msg);
    }

    public GridPane createChat() {

        TextField enterMessageField = new TextField();
        enterMessageField.setEditable(true);
        enterMessageField.setMinWidth(350);

        displayAllMessages.setPrefHeight(800);
        displayAllMessages.setEditable(false);
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(displayAllMessages);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        displayAllMessages.setMinWidth(400);

        Button button = new Button("Send");
        button.setMinWidth(50);
        button.setDefaultButton(true);

        VBox vBoxChatIncoming = new VBox(displayAllMessages);
        vBoxChatIncoming.setPadding(new Insets(10, 10, 10, 10));

        HBox hBoxSend = new HBox(10, enterMessageField, button);
        hBoxSend.setPadding(new Insets(10, 10, 10, 10));

        GridPane rootPane = new GridPane();
        rootPane.add(vBoxChatIncoming, 0, 0);
        rootPane.add(hBoxSend, 0, 1);

        enterMessageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String s = enterMessageField.getText() + "\n";
                enterMessageField.setText("");
                if (!s.isBlank()) {
                    send("communication:" +s);
                    addMessage(username+": "+s);
                }
            }
        });

        button.setOnAction((event) -> {
            String s = enterMessageField.getText() + "\n";
            enterMessageField.setText("");
            if (!s.isBlank()) {
                send("communication:" +s);
                addMessage(username+": "+s);
            }
        });

        return rootPane;
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

        globalMessage = bufferedReader.readLine();
        System.out.println(globalMessage);
        listenForMessage();
        chatWriter();
    }
    public void loginScreen(Stage stage){
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

                                if (globalMessage.equals("login error")) {
                                    a = new Alert(Alert.AlertType.ERROR);
                                    a.setHeaderText("User already logged in!");
                                    a.setTitle("Access denied!");
                                    a.showAndWait();
                                } else if (globalMessage.equals("login successful"))
                                    buildScene();
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
                stage.setResizable(false);
                stage.setTitle("Battleships");
                stage.show();
            } catch (Exception e) {
                System.out.println("BŁĄD jakiś");
                e.printStackTrace();
            }
        });
    }
    public void init(Stage stage) {
        Platform.runLater(() -> {
            try {
                BorderPane mainScreenPane=new BorderPane();
                HBox title=new HBox();
                title.setAlignment(Pos.CENTER);
                // Text titleText=;
                Text titleText=new Text("Battleships");
                titleText.setFont(Font.font("system",36));
                title.getChildren().add(titleText);
                HBox.setMargin(title,new Insets(0,105,0,0));

                //title.setMinWidth(400);

                Button startButton=new Button("Rozpocznij grę");
                startButton.setOnAction(event -> {
                    loginScreen(stage);
                });
                HBox buttonHbox=new HBox(startButton);
                // buttonHbox.setMinWidth(100);
                buttonHbox.setAlignment(Pos.CENTER);

                HBox.setMargin(buttonHbox,new Insets(11,20,0,0));
                // buttonHbox.maxHeight(50);
                // buttonHbox.setAlignment(Pos.CENTER);
                HBox topWrap=new HBox(title,buttonHbox);
                topWrap.setAlignment(Pos.CENTER_RIGHT);
                topWrap.setPrefHeight(150);
                mainScreenPane.setTop(topWrap);
                TableView table=new TableView<UserRankingDTO>();
                TableColumn place=new TableColumn<>("Miejsce w rankingu");
                place.setCellValueFactory(new PropertyValueFactory<>("number"));
                place.setReorderable(false);
                place.setSortable(false);
                place.setPrefWidth(128);
                TableColumn name=new TableColumn<>("Nazwa gracza");
                name.setCellValueFactory(new PropertyValueFactory<>("username"));
                name.setReorderable(false);
                name.setSortable(false);
                name.setPrefWidth(110);
                TableColumn winNumber=new TableColumn<>("Ilość wygranych");
                winNumber.setCellValueFactory(new PropertyValueFactory<>("gameWin"));
                winNumber.setReorderable(false);
                winNumber.setSortable(false);
                winNumber.setPrefWidth(106);
                table.getColumns().addAll(place,name,winNumber);
                HBox ranking=new HBox(table);
                ranking.setAlignment(Pos.CENTER);
                //HBox.setMargin(ranking,new Insets(0,0,0,20));
                HBox spaceFill=new HBox();
                spaceFill.setPrefSize(600,100);
                //
                List<UserRankingDTO> rankingList= GameController.getInstance().findTopTenPlayers();
                for(UserRankingDTO user:rankingList){
                    table.getItems().add(user);
                }
                mainScreenPane.setBottom(spaceFill);
                mainScreenPane.setCenter(ranking);

                stage.setScene(new Scene(mainScreenPane,600,750));
                stage.setTitle("Battleships");
                stage.setResizable(false);
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
            this.primaryStage.setTitle("Battleships - " + username);
            this.primaryStage.setScene(scene);
            this.primaryStage.setResizable(false);
            this.primaryStage.show();
        });
    }


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        Platform.runLater(() -> {
            try {
                HibernateUtil.getSessionFactory().openSession();
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
        HibernateUtil.getSessionFactory().close();
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
        } catch (NullPointerException e) {
            System.out.println("Message is null, cannot send");
        }
    }


    public void checkHit(String cords) {
        Cell cell = playerBoard.getCell(cords.charAt(0) - '0', cords.charAt(1) - '0');
        System.out.println("Sprawdz pole: " + cell.x + cell.y);
        int state = cell.shoot();
        if (state == 0) {
            send("hit");
        } else if(state == 1){
            send("hit");

            if (playerBoard.ships == 0) {
                GameController.getInstance().updateRanking(this.username, "lose");
                send("win");
                winStatus = "GAME LOST";
                System.out.println("GAME LOST");
                enemyTurn = true;
                endGameScreen();
            } else {
                send("sunk");
                addMessage("Your ship has been sunk.\n");
            }

        } else if(state == -1) {
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

                popupStage.setResizable(false);
                popupStage.show();
            } catch (Exception e) {
                System.out.println("BŁĄD jakiś");
                e.printStackTrace();
            }
        });
    }

    // send chat message with specific prefix
    public void chatWriter() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String chatMsgToSend;
                Scanner scanner = new Scanner(System.in);
                while (socket.isConnected()) {
                    chatMsgToSend = scanner.nextLine();
                    send("communication:" + chatMsgToSend);
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
                        System.out.println("Raw Message: " + msgFromGroupChat);


                        // if someone left, and the game has no result (someone left during game) and was started then restart game
                        if (msgFromGroupChat == null || (msgFromGroupChat.equals("left") && winStatus == null && start)) {
                            addMessage("Your enemy left - restarting table\n");
                            restartGame();
                            continue;
                        }

                        // user already logged in
//                        if (msgFromGroupChat.equals("login error")){
//                            globalMessage = "login error";
//                            continue;
//                        }

                        // enemy left before game start
                        if (msgFromGroupChat.equals("left") && !(winStatus == null && start))
                            addMessage("Your enemy left - waiting for another to join\n");

                        // chat communication
                        if (msgFromGroupChat.startsWith("communication:")) {
                            addMessage(msgFromGroupChat.substring(14)+"\n");
                            continue;
                        }

                        // messages to check before game starts
                        if (!start) {
                            switch (msgFromGroupChat) {
                                case "first":
                                    addMessage("New Game Started\n");
                                    addMessage("You Start First\nWaiting for enemy to place ships...\n");
                                    enemyTurn = false;
                                    // placing ships blocked until two players at the table
                                    mayPlaceShips = true;
                                    break;

                                case "second":
                                    addMessage("New Game Started\n");
                                    addMessage("You Start Second\nWaiting for enemy to place ships...\n");
                                    enemyTurn = true;
                                    // placing ships blocked until two players at the table
                                    mayPlaceShips = true;
                                    break;

                                case "Your opponent has finished placing ships":
                                    addMessage("Your opponent has finished placing ships - Good Luck\n");
                                    start = true;
                                    break;
                            }
                        }

                        // messages to check when game is running
                        if (start) {

                            if (msgFromGroupChat.length() == 2) {                                   // board coordinates
                                checkHit(msgFromGroupChat);
                            } else {                                                                // commands
                                switch (msgFromGroupChat) {

                                    case "end":
                                        enemyTurn = false;
                                        break;

                                    case "sunk":
                                    case "hit":
                                    case "miss":
                                        lock.lock();
                                        try {
                                            if(msgFromGroupChat.equals("sunk"))
                                                addMessage("Sunk an enemy ship.\n");
                                            globalMessage = msgFromGroupChat;
                                        } finally {
                                            lock.unlock();
                                        }
                                        break;

                                    case "win":
                                        GameController.getInstance().updateRanking(username, "win");
                                        addMessage("YOU WIN");
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