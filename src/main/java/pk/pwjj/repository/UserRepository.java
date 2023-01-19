package pk.pwjj.repository;

import pk.pwjj.HibernateUtil;
import pk.pwjj.entity.Ranking;
import pk.pwjj.entity.User;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;



public class UserRepository {

    private static UserRepository userRepository;

    private UserRepository(){
    }

    public static UserRepository getInstance(){
        if(userRepository == null)
            userRepository = new UserRepository();
        return userRepository;
    }


    public Optional<User> findUserByUsername(String username){
        var session = HibernateUtil.getSessionFactory().getCurrentSession();
        var transaction = session.beginTransaction();

        Optional<User> result = Optional.empty();
        try {
            Query query = session.createQuery("SELECT u FROM User u LEFT JOIN FETCH u.ranking WHERE u.username=:username");
            query.setParameter("username", username);
            result = Optional.ofNullable((User) query.getSingleResult());
            transaction.commit();
        }catch (NoResultException e){
            transaction.commit();
            System.out.println("Uzytkownik o takiej nazwie uzytkownika nie istnieje");
            return result;
        }finally {
            session.close();
        }

        return result;
    }

    public void addUser(User user){
        var session = HibernateUtil.getSessionFactory().getCurrentSession();
        var transaction = session.beginTransaction();

        try{
            session.save(user);
            transaction.commit();
        }catch(Exception e){
            transaction.rollback();
            e.printStackTrace();
        }
        finally {
            session.close();
        }
    }


    public void updateRanking(Ranking ranking){
        var session = HibernateUtil.getSessionFactory().getCurrentSession();
        var transaction = session.beginTransaction();

        try{
            session.update(ranking);
            transaction.commit();
        }catch (Exception e){
            transaction.rollback();
            e.printStackTrace();
        }finally {
            session.close();
        }
    }

    public List<User> findTopTenPlayers(){
        var session = HibernateUtil.getSessionFactory().getCurrentSession();
        var transaction = session.beginTransaction();

        List<User> result = new ArrayList<>();
        try{
            Query query = session.createQuery("SELECT u FROM User u LEFT JOIN FETCH u.ranking ORDER BY u.ranking.gameWin DESC ").setMaxResults(10);
            result = (List<User>) query.getResultList();
            transaction.commit();
        }catch (Exception e){
            transaction.rollback();
            e.printStackTrace();
            return result;
        }finally {
            session.close();
        }

        return result;
    }

}
