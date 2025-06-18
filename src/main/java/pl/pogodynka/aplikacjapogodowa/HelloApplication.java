package pl.pogodynka.aplikacjapogodowa;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

// Główna klasa aplikacji JavaFX, odpowiedzialna za uruchomienie interfejsu graficznego
public class HelloApplication extends Application {

    // Referencja do kontrolera głównego, używana do wywoływania metod przy zamykaniu aplikacji
    private MainController mainController;

    // Metoda startująca aplikację, inicjalizuje okno główne
    @Override
    public void start(Stage stage) throws IOException {
        // Ładowanie pliku FXML definiującego układ interfejsu użytkownika
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 430, 580);

        // Pobieranie instancji kontrolera z załadowanego pliku FXML
        mainController = fxmlLoader.getController();

        // Dodanie arkusza stylów CSS do sceny
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        // Ustawienie tytułu okna
        stage.setTitle("Aplikacja Pogodowa - JavaFX");
        // Ustawienie sceny w oknie głównym
        stage.setScene(scene);
        // Wyłączenie możliwości zmiany rozmiaru okna
        stage.setResizable(false);
        // Wyświetlenie okna
        stage.show();
    }

    // Metoda wywoływana przy zamykaniu aplikacji
    @Override
    public void stop() throws Exception {
        super.stop();
        // Wywołanie metody shutdown kontrolera, jeśli istnieje
        if (mainController != null) {
            mainController.shutdown();
        }
    }

    // Punkt wejścia aplikacji
    public static void main(String[] args) {
        // Uruchomienie aplikacji JavaFX
        launch();
    }
}