package pk.pwjj.controller;

import com.password4j.BcryptFunction;
import com.password4j.Password;
import com.password4j.types.Bcrypt;
import pk.pwjj.entity.Ranking;
import pk.pwjj.entity.User;
import pk.pwjj.repository.UserRepository;

import java.util.Optional;

/**
 * LoginController class provides basic functions that help users with the login process
 */
public class LoginController {

    /**
     * The constant that is used to store LoginController object
     */
    private static LoginController loginController;

    private LoginController() {
    }

    /**
     * Function that returns an instance of the LoginController class
     *
     * @return an instance of the LoginController class
     */
    public static LoginController getInstance() {
        if (loginController == null)
            loginController = new LoginController();
        return loginController;
    }

    /**
     * Function that returns specific value
     *
     * @param username username of particular user
     * @param password password of particular user
     * @return 0 - user has been created, 1 - user has logged in, -1 - wrong password
     */
    public Integer login(String username, String password) {
        Optional<User> userOptional = UserRepository.getInstance().findUserByUsername(username);

        if (userOptional.isEmpty()) {
            Ranking ranking = new Ranking(0, 0);
            User user = new User(username, Password.hash(password).with(BcryptFunction.getInstance(Bcrypt.Y, 10)).getResult(), ranking);
            ranking.setUser(user);
            UserRepository.getInstance().addUser(user);
            return 0;
        }

        User user = userOptional.get();

        if (checkCorrectPassword(password, user.getPassword()))
            return 1;
        return -1;
    }

    /**
     * Function that checks the correctness of the given password
     *
     * @param givenPassword   password provided by the user
     * @param correctPassword password from the database
     * @return false - an invalid password was given, true - the correct password was given
     */
    private boolean checkCorrectPassword(String givenPassword, String correctPassword) {
        BcryptFunction bcrypt = BcryptFunction.getInstanceFromHash(correctPassword);
        if (!Password.check(givenPassword, correctPassword).with(bcrypt))
            return false;
        return true;
    }

}
