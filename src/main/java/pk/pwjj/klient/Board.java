package pk.pwjj.klient;

import java.util.ArrayList;
import java.util.List;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Board that players can play on.
 */
public class Board extends Parent {
    private VBox rows = new VBox();
    /** indicates if board belongs to current user or enemy*/
    private boolean enemy = false;
    /** number ships that are still on board*/
    public int ships = 5;

    /**
     * Initiates Board.
     * @param enemy variable for checking if there are 2 players on certain board
     * @param handler event handler for actions on board
     */
    public Board(boolean enemy, EventHandler<? super MouseEvent> handler) {
        this.enemy = enemy;
        HBox title=new HBox();
        title.setAlignment(Pos.CENTER);
        Text titleText=new Text();

        titleText.setFont(Font.font("verdana", FontWeight.BOLD,15));
        if(enemy==false) {
            titleText.setText("My ships");
            title.getChildren().add(titleText);
            title.setPadding(new Insets(0,0,0,30));
        }
        else {
            titleText.setText("Opponent's ships");
            title.getChildren().add(titleText);
            title.setPadding(new Insets(0,0,0,35));
        }
        rows.getChildren().add(title);
        char[] alphabet = "ABCDEFGHIJ".toCharArray();
        for (int y = 0; y < 10; y++) {
            HBox row = new HBox();

            HBox info=new HBox();
            Text cellInfo=new Text(String.valueOf(alphabet[y]));
            cellInfo.setFont(Font.font(15));
            info.getChildren().add(cellInfo);
            info.setAlignment(Pos.CENTER);
            info.setPadding(new Insets(0,10,0,0));
            info.setPrefSize(30,30);
            row.getChildren().add(info);

            for (int x = 0; x < 10; x++) {
                Cell c = new Cell(x, y, this);
                c.setOnMouseClicked(handler);
                row.getChildren().add(c);
            }

            rows.getChildren().add(row);
        }

        HBox row = new HBox();
        HBox info=new HBox();
        Text cellInfo=new Text(String.valueOf(" ".toString()));
        cellInfo.setFont(Font.font(15));
        info.getChildren().add(cellInfo);
        info.setAlignment(Pos.CENTER);
       // info.setPadding(new Insets(0,10,0,0));
        info.setPrefSize(30,30);
        row.getChildren().add(info);
        for(int x=1;x<11;x++) {
            info=new HBox();
            info.setPrefSize(31,30);
            Text infoText=new Text(String.valueOf(x));
            infoText.setFont(Font.font(15));
            info.getChildren().add(infoText);
            info.setAlignment(Pos.CENTER);
            row.getChildren().add(info);
        }
        rows.getChildren().add(row);
        getChildren().add(rows);
    }

    /**
     * Placing ship on board.
     * @param ship Ship instance to place
     * @param x coordinate
     * @param y coordinate
     * @return returns true if ship has been placed
     */
    public boolean placeShip(Ship ship, int x, int y) {
        if (canPlaceShip(ship, x, y)) {
            int length = ship.type;

            if (ship.vertical) {
                for (int i = y; i < y + length; i++) {
                    Cell cell = getCell(x, i);
                    cell.ship = ship;
                    if (!enemy) {
                        cell.setFill(Color.WHITE);
                        cell.setStroke(Color.GREEN);
                    }
                }
            }
            else {
                for (int i = x; i < x + length; i++) {
                    Cell cell = getCell(i, y);
                    cell.ship = ship;
                    if (!enemy) {
                        cell.setFill(Color.WHITE);
                        cell.setStroke(Color.GREEN);
                    }
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Getting cell under that is placed under provided coordinates.
     * @param x coordinate
     * @param y coordinate
     * @return cell from given coordinates
     */
    public Cell getCell(int x, int y) {
        if (isValidPoint(x,y))
            return (Cell)((HBox)rows.getChildren().get(y+1)).getChildren().get(x+1);
        else
            return null;
    }

    /**
     * Returns all neighbours of given cell cooridanates.
     * @param x coordinate
     * @param y coordinate
     * @return neighbours of given parameters in array
     */
    private Cell[] getNeighbors(int x, int y) {
        Point2D[] points = new Point2D[] {
                new Point2D(x - 1, y),
                new Point2D(x + 1, y),
                new Point2D(x, y - 1),
                new Point2D(x, y + 1)
        };

        List<Cell> neighbors = new ArrayList<Cell>();

        for (Point2D p : points) {
            if (isValidPoint(p)) {
                neighbors.add(getCell((int)p.getX(), (int)p.getY()));
            }
        }

        return neighbors.toArray(new Cell[0]);
    }

    /**
     * Checks if certain ship can be placed.
     * @param ship ship to place
     * @param x coordinate
     * @param y coordinate
     * @return returns True if ship can be placed, False if it can not
     */
    private boolean canPlaceShip(Ship ship, int x, int y) {
        int length = ship.type;

        if (ship.vertical) {
            for (int i = y; i < y + length; i++) {
                if (!isValidPoint(x, i))
                    return false;

                Cell cell = getCell(x, i);
                if (cell.ship != null)
                    return false;

                for (Cell neighbor : getNeighbors(x, i)) {
                    if (!isValidPoint(x, i))
                        return false;

                    if (neighbor.ship != null)
                        return false;
                }
            }
        }
        else {
            for (int i = x; i < x + length; i++) {
                if (!isValidPoint(i, y))
                    return false;

                Cell cell = getCell(i, y);
                if (cell.ship != null)
                    return false;

                for (Cell neighbor : getNeighbors(i, y)) {
                    if (!isValidPoint(i, y))
                        return false;

                    if (neighbor.ship != null)
                        return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks if point is valid.
     * @param point that action is performed on to
     * @return returns True if point is valid, False if point is invalid
     */
    private boolean isValidPoint(Point2D point) {
        return isValidPoint(point.getX(), point.getY());
    }

    /**
     * Checks if given coordinates are valid.
     * @param x coordinate
     * @param y coordinate
     * @return return True if point is inside field, False if point is not inside field
     */
    private boolean isValidPoint(double x, double y) {
        return x >= 0 && x < 10 && y >= 0 && y < 10;
    }

    /**
     * Class that represents Cell in game.
     */
    public class Cell extends Rectangle {
        public int x, y;
        public Ship ship = null;
        public boolean wasShot = false;

        private Board board;

        /**
         * Initiating Cell.
         * @param x coordinate
         * @param y coordinate
         * @param board that Cell is being placed on to
         */
        public Cell(int x, int y, Board board) {
            super(30, 30);
            this.x = x;
            this.y = y;
            this.board = board;
            setFill(Color.LIGHTGRAY);
            setStroke(Color.BLACK);
        }

        /**
         * Shoot performed on Cell instance.
         * @return 1 if ship had sunk, 0 if it is hit, -1 if it is miss
         */
        public int shoot() {
            wasShot = true;
            setFill(Color.BLACK);

            if (ship != null) {
                ship.hit();
                setFill(Color.RED);
                if (!ship.isAlive()) {
                    board.ships--;
                    return 1;
                }
                return 0;
            }

            return -1;
        }
    }
}