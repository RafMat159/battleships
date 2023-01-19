package pk.pwjj.controller;

import com.password4j.BcryptFunction;
import com.password4j.Password;
import com.password4j.types.Bcrypt;
import pk.pwjj.entity.Ranking;
import pk.pwjj.entity.User;
import pk.pwjj.repository.UserRepository;

import java.util.Optional;

public class LoginController {

    private static LoginController loginController;

    private LoginController(){
    }

    public static LoginController getInstance(){
        if(loginController == null)
            loginController = new LoginController();
        return loginController;
    }

    public Integer login(String username, String password) {
        Optional<User> userOptional = UserRepository.getInstance().findUserByUsername(username);

        if(userOptional.isEmpty()){
            Ranking ranking = new Ranking(0,0);
            User user = new User(username,Password.hash(password).with(BcryptFunction.getInstance(Bcrypt.Y,10)).getResult(),ranking);
            ranking.setUser(user);
            UserRepository.getInstance().addUser(user);
            return 0;
        }

        User user = userOptional.get();

        if(checkCorrectPassword(password,user.getPassword()))
            return 1;
        return -1;
    }

    private boolean checkCorrectPassword(String givenPassword, String correctPassword){
        BcryptFunction bcrypt = BcryptFunction.getInstanceFromHash(correctPassword);
        if(!Password.check(givenPassword,correctPassword).with(bcrypt))
            return false;
        return true;
    }

}
