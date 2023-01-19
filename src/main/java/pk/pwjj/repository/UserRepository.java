package pk.pwjj.repository;

import pk.pwjj.HibernateUtil;
import pk.pwjj.entity.Ranking;
import pk.pwjj.entity.User;

import javax.persistence.NoResultException;
import javax.persistence.Query;
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
        var trasaction = session.beginTransaction();

        Optional<User> result = Optional.empty();
        try {
            Query query = session.createQuery("SELECT u FROM User u WHERE u.username=:username");
            query.setParameter("username", username);
            result = Optional.ofNullable((User) query.getSingleResult());
        }catch (NoResultException e){
            trasaction.commit();
            session.close();
            return result;
        }
        trasaction.commit();
        session.close();
        return result;
    }

    public Optional<User> findUserByUsernameWithRanking(String username){
        var session = HibernateUtil.getSessionFactory().getCurrentSession();
        var trasaction = session.beginTransaction();

        Optional<User> result = Optional.empty();
        try {
            Query query = session.createQuery("SELECT u FROM User u LEFT JOIN FETCH u.ranking WHERE u.username=:username");
            query.setParameter("username", username);
            result = Optional.ofNullable((User) query.getSingleResult());
        }catch (NoResultException e){
            trasaction.commit();
            session.close();
            return result;
        }
        trasaction.commit();
        session.close();
        return result;
    }


    public void addUser(User user){
        var session = HibernateUtil.getSessionFactory().getCurrentSession();
        var trasaction = session.beginTransaction();

        try{
            session.save(user);
            trasaction.commit();
        }catch(Exception e){
            e.printStackTrace();
        }
        finally {
            session.close();
        }
    }


    public void updateRanking(Ranking ranking){
        var session = HibernateUtil.getSessionFactory().getCurrentSession();
        var trasaction = session.beginTransaction();

        try{
            session.update(ranking);
            trasaction.commit();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            session.close();
        }
    }




//update liczbawygranych lub liczbaprzegranych
}
