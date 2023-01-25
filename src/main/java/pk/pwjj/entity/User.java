package pk.pwjj.entity;

import javax.persistence.*;

/**
 * Entity User
 * */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    /**
     * Hibernate (JPA) needs it.
     */
    @SuppressWarnings("unused")
    public User() {
    }

    public User(String username, String password, Ranking ranking) {
        this.username = username;
        this.password = password;
        this.ranking = ranking;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private Ranking ranking;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Ranking getRanking() {
        return ranking;
    }

    public void setRanking(Ranking ranking) {
        this.ranking = ranking;
    }
}