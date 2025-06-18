module pl.pogodynka.aplikacjapogodowa {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.net.http;

    opens pl.pogodynka.aplikacjapogodowa to javafx.fxml;
    opens pl.pogodynka.aplikacjapogodowa.model to com.google.gson;

    exports pl.pogodynka.aplikacjapogodowa.model;
    exports pl.pogodynka.aplikacjapogodowa;
}