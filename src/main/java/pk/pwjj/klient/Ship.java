package pk.pwjj.klient;

import javafx.scene.Parent;

/**
 * Single ship instance.
 */
public class Ship extends Parent {
    /** ship number for certain board*/
    public int type;
    /** type of ship layout*/
    public boolean vertical = true;
    /** health of ship*/
    private int health;

    /**
     * Initiates ship.
     * @param type type of ship (String number).
     * @param vertical type of ship. Ship can be vertical (True)  or horizontal (False).
     */
    public Ship(int type, boolean vertical) {
        this.type = type;
        this.vertical = vertical;
        health = type;
    }

    /**
     * Decreases Ship health.
     */
    public void hit() {
        health--;
    }

    /**
     * Checks if ship is still alive.
     * @return Returns True if Ship is still alive, False if it is not.
     */
    public boolean isAlive() {
        return health > 0;
    }
}