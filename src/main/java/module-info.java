module pk.pwjj.klient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;

    opens pk.pwjj.klient to javafx.fxml;
    exports pk.pwjj.klient;
}