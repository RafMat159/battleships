package pk.pwjj.klient;

import java.util.HashMap;

public class GameController {

    // true value means that there is a ship on given field
    private HashMap<String, Boolean> ownGameBoard = new HashMap<>();
    private HashMap<String, Boolean> enemyGameBoard = new HashMap<>();
    private int shipsLeft;

    public GameController(){
        int litera = 65;
//        for (int i = 1; i < 10; i++){
//            for (int j = 1; j < 10; j++) {
//                String cords  = (char)litera+""+j;
//                ownGameBoard.put(cords, false);
////                enemyGameBoard.put(cords, false);
//            }
//            litera++;
//        }


        /// TEST PURPOSEES
        ownGameBoard.put("00", true);
        ownGameBoard.put("01", true);
//        ownGameBoard.put("C4", true);
//        ownGameBoard.put("D4", true);
//        ownGameBoard.put("E4", true);
//        ownGameBoard.put("F7", true);
//        ownGameBoard.put("F6", true);


        // hit enemy ships
        enemyGameBoard.put("56", true);
        enemyGameBoard.put("55", true);

        // missed enemy ships
        enemyGameBoard.put("26", false);
        enemyGameBoard.put("27", false);

        shipsLeft = ownGameBoard.size();
    }

    public void printOwnBoard(){
        int litera = 65;
        boolean czyStatek;
        char znak;
        System.out.println("--- PLAYER BOARD ---");
        for (int i = 1; i < 10; i++){
            for (int j = 1; j < 10; j++) {
                String cords  = i+""+j;
                if (ownGameBoard.get(cords) == null) {
                    znak = '0';
                } else{
                    znak = ownGameBoard.get(cords) ? 's' : 'x';
                }
                System.out.print(znak+" ");
            }
            litera++;
            System.out.println();
        }
    }

    public void printEnemyBoard(){
        int litera = 65;
        boolean czyStatek;
        char znak;
        System.out.println("--- ENEMY BOARD ---");
        for (int i = 1; i < 10; i++){
            for (int j = 1; j < 10; j++) {
                String cords  = i+""+j;

                if (enemyGameBoard.get(cords) == null) {
                    znak = '0';
                }
                else {
                    czyStatek = enemyGameBoard.get(cords);
                    znak = czyStatek ? 'x' : '-';
                }
                System.out.print(znak+" ");
            }
            litera++;
            System.out.println();
        }
    }

    // check if enemy guessed correctly coordinates
    public String checkIfHit(String cords){
        if(ownGameBoard.get(cords) != null) {
            ownGameBoard.put(cords, false);
            shipsLeft--;
            if (shipsLeft <= 0)
                return "end";
            return "hit";
        }
        return "miss";
    }

    public void addToEnemyBoard(String[] cordsAndVal){
        boolean hit = false;
        if (cordsAndVal[1].equals("hit"))
            hit = true;
        else
            hit = false;
        enemyGameBoard.put(cordsAndVal[0], hit);
    }
    public static void main(String[] args) {
        GameController game = new GameController();
        game.printOwnBoard();
        System.out.println("----------------");
        game.printEnemyBoard();
        System.out.println("----------------");
        System.out.println(game.checkIfHit("A5"));
    }
}
