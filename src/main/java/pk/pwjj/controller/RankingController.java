package pk.pwjj.controller;

import pk.pwjj.DTO.UserRankingDTO;
import pk.pwjj.entity.Ranking;
import pk.pwjj.entity.User;
import pk.pwjj.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The RankingController class provides basic functions that show ranking
 * */
public class RankingController {

    /**The constant that is used to store RankingController object*/
    private static RankingController rankingController;

    private RankingController(){
    }

    /**
     * Function that returns an instance of the RankingController class
     * @return an instance of the RankingController class
     * */
    public static RankingController getInstance(){
        if(rankingController == null)
            rankingController = new RankingController();
        return rankingController;
    }

    /**
     * Function that updates ranking
     * @param username username of particular user
     * @param message message from server
     * @return 0 - successful operation 1 - unsuccessful operation
     * */
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

    /**
     * Function that returns the top ten players from the database
     * @return list of top ten users
     * */
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

