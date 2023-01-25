package pk.pwjj;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * HibernateUtil class that builds and closes session factory
 */
public class HibernateUtil {

    /**The constant that is used to store SessionFactory object*/
    private static final SessionFactory sessionFactory = buildSessionFactory();

    /**
     * Function that closes the session factory
     * */
    static void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    /**
     * Function that returns an instance of the SessionFactory class
     * @return an instance of the SessionFactory class
     * */
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Function that builds SessionFactory
     * @return built SessionFactory
     * */
    private static SessionFactory buildSessionFactory() {
        // A SessionFactory is set up once for an application!
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure() // configures settings from hibernate.cfg.xml
                .build();
        try {
            return new MetadataSources(registry).buildMetadata().buildSessionFactory();
        } catch (Exception e) {
            // The registry would be destroyed by the SessionFactory, but we had trouble building the SessionFactory
            // so destroy it manually.
            StandardServiceRegistryBuilder.destroy(registry);
            throw e;
        }
    }

    /**
     * Hibernate (JPA) needs it.
     * */
    private HibernateUtil() {
    }

}
