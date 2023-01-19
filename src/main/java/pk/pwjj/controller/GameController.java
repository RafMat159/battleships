package pk.pwjj.controller;

import pk.pwjj.DTO.UserRankingDTO;
import pk.pwjj.entity.Ranking;
import pk.pwjj.entity.User;
import pk.pwjj.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameController {

    private static GameController gameController;

    private GameController(){
    }

    public static GameController getInstance(){
        if(gameController == null)
            gameController = new GameController();
        return gameController;
    }

    public Integer updateRanking(String username, String message) {
        Optional<User> userOptional = UserRepository.getInstance().findUserByUsername(username);

        if (userOptional.isPresent()) {
            Ranking ranking = userOptional.get().getRanking();

            if (message.equals("win")) {
                ranking.setGameWin(ranking.getGameWin() + 1);
            } else if (message.equals("lose")) {
                ranking.setGameLost(ranking.getGameLost() + 1);
            }

            UserRepository.getInstance().updateRanking(ranking);
            return 0;
        }
        return -1;
    }

    public List<UserRankingDTO> findTopTenPlayers(){
        List<User> users = UserRepository.getInstance().findTopTenPlayers();
        List<UserRankingDTO> userRankingDTOS = new ArrayList<>();
        int i = 0;
        for (User user: users) {
            userRankingDTOS.add(new UserRankingDTO(++i,user.getUsername(),user.getRanking().getGameWin()));
        }
        return userRankingDTOS;
    }

}

