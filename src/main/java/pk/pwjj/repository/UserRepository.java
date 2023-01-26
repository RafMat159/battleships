package pk.pwjj.repository;

import pk.pwjj.HibernateUtil;
import pk.pwjj.entity.Ranking;
import pk.pwjj.entity.User;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/***
 * The UserRepository class is responsible for sending queries to the database and receiving the results
 * */
public class UserRepository {

    /**
     * The constant that is used to store UserRepository object
     */
    private static UserRepository userRepository;

    private UserRepository() {
    }

    /**
     * Function that returns an instance of the UserRepository class
     *
     * @return an instance of the UserRepository class
     */
    public static UserRepository getInstance() {
        if (userRepository == null)
            userRepository = new UserRepository();
        return userRepository;
    }

    /**
     * Function that returns an object of the User class
     *
     * @param username username of particular user
     * @return a specific user or Optional.empty if the user doesn't exist in the database
     */
    public Optional<User> findUserByUsername(String username) {
        var session = HibernateUtil.getSessionFactory().getCurrentSession();
        var transaction = session.beginTransaction();

        Optional<User> result = Optional.empty();
        try {
            Query query = session.createQuery("SELECT u FROM User u LEFT JOIN FETCH u.ranking WHERE u.username=:username");
            query.setParameter("username", username);
            result = Optional.ofNullable((User) query.getSingleResult());
            transaction.commit();
        } catch (NoResultException e) {
            transaction.commit();
            System.out.println("Uzytkownik o takiej nazwie uzytkownika nie istnieje");
            return result;
        } finally {
            session.close();
        }

        return result;
    }


    /**
     * Function that adds a user to the database
     *
     * @param user an object of the User class
     */
    public void addUser(User user) {
        var session = HibernateUtil.getSessionFactory().getCurrentSession();
        var transaction = session.beginTransaction();

        try {
            session.save(user);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    /**
     * Function that updates a ranking
     *
     * @param ranking an object of the Ranking class
     */
    public void updateRanking(Ranking ranking) {
        var session = HibernateUtil.getSessionFactory().getCurrentSession();
        var transaction = session.beginTransaction();

        try {
            session.update(ranking);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    /**
     * Function that returns the top ten players from the database
     *
     * @return list of top ten users
     */
    public List<User> findTopTenPlayers() {
        var session = HibernateUtil.getSessionFactory().getCurrentSession();
        var transaction = session.beginTransaction();

        List<User> result = new ArrayList<>();
        try {
            Query query = session.createQuery("SELECT u FROM User u LEFT JOIN FETCH u.ranking ORDER BY u.ranking.gameWin DESC ").setMaxResults(10);
            result = (List<User>) query.getResultList();
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
            return result;
        } finally {
            session.close();
        }

        return result;
    }

}
