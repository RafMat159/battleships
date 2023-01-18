package pk.pwjj.repository;

import pk.pwjj.HibernateUtil;
import pk.pwjj.entity.User;

import javax.persistence.Query;
import java.util.Optional;



public class UserRepository {

    public Optional<User> findUser(String username){
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
        }catch(Exception e){
            e.printStackTrace();
        }

        trasaction.commit();
        session.close();
    }






//update liczbawygranych lub liczbaprzegranych
}
