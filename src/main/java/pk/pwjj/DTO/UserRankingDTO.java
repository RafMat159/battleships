package pk.pwjj.DTO;

/**
 * UserRankingDTO class that is used to transform data
 */
public class UserRankingDTO {

    private Integer number;
    private String username;
    private Integer gameWin;

    public UserRankingDTO(Integer number, String username, Integer gameWin) {
        this.number = number;
        this.username = username;
        this.gameWin = gameWin;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getGameWin() {
        return gameWin;
    }

    public void setGameWin(Integer gameWin) {
        this.gameWin = gameWin;
    }
}
