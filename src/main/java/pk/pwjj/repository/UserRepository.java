package pk.pwjj.repository;

import pk.pwjj.HibernateUtil;
import pk.pwjj.entity.User;

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

        Query query = session.createQuery("SELECT u.username FROM User u WHERE u.username=:username");
        query.setParameter("username",username);
        var result = Optional.ofNullable((User)query.getSingleResult());

        trasaction.commit();
        session.close();
        return result;
    }

    public void addUser(User user){
        var session = HibernateUtil.getSessionFactory().getCurrentSession();
        var trasaction = session.beginTransaction();

        try{
            session.save(user);
            session.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        finally {
            trasaction.commit();
        }
    }






//update liczbawygranych lub liczbaprzegranych
}
