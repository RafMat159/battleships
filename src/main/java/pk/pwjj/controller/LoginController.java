package pk.pwjj.controller;

import com.password4j.BcryptFunction;
import com.password4j.HashChecker;
import com.password4j.types.Bcrypt;
import pk.pwjj.entity.Ranking;
import pk.pwjj.entity.User;
import pk.pwjj.repository.UserRepository;

import com.password4j.Password;
import java.util.Optional;

public class LoginController {

    public Integer login(String username, String password) {
        Optional<User> userOptional = UserRepository.getInstance().findUserByUsername(username);

        if(userOptional.isEmpty()){
            Ranking ranking = new Ranking(0,0);
            User user = new User(username,Password.hash(password).with(BcryptFunction.getInstance(Bcrypt.Y,10)).getResult(),ranking);
            UserRepository.getInstance().addUser(user);
            return 0;
        }

        User user = userOptional.get();

        if(correctPassword(password,user.getPassword())){
            return -1;
        }
        return 1;
    }

    private boolean correctPassword(String givenPassword, String correctPassword){
        HashChecker hashChecker = Password.check(givenPassword,correctPassword);
        if(hashChecker.withBcrypt()){
            return true;
        }
        return false;
    }

}
