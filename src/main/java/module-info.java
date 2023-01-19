open module pk.pwjj.klient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;
    requires java.naming;
    requires password4j;
    requires password4j.jca;
    //requires org.slf4j;
    requires dom4j;
    requires java.sql;

  //  requires hibernate.jpa;
    requires org.hibernate.orm.core;
    requires java.persistence;

    exports pk.pwjj.klient;
}