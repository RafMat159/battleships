package pk.pwjj.entity;

import javax.persistence.*;

@Entity
@Table(name = "ranking")
public class Ranking {

    @Id
    @Column(name = "user_id")
    private Integer id;

    @Column(name = "game_lost")
    private Integer gameLost;

    @Column(name = "game_win")
    private Integer gameWin;

    /**
     * Hibernate (JPA) needs it.
     * */
    @SuppressWarnings("unused")
    public Ranking() {
    }

    public Ranking(Integer gameLost, Integer gameWin, User user) {
        this.gameLost = gameLost;
        this.gameWin = gameWin;
        this.user = user;
    }

    public Ranking(Integer gameLost, Integer gameWin) {
        this.gameLost = gameLost;
        this.gameWin = gameWin;
    }

    @OneToOne(cascade = CascadeType.ALL)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    public Integer getGameLost() {
        return gameLost;
    }

    public void setGameLost(Integer gameLost) {
        this.gameLost = gameLost;
    }

    public Integer getGameWin() {
        return gameWin;
    }

    public void setGameWin(Integer gameWin) {
        this.gameWin = gameWin;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
