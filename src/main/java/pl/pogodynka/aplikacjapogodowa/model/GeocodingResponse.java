package pl.pogodynka.aplikacjapogodowa.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// Klasa modelowa przechowująca odpowiedź z API geokodowania Open-Meteo
public class GeocodingResponse {
    // Lista wyników geokodowania (lokalizacji)
    private List<Location> results;
    // Czas generowania odpowiedzi w milisekundach
    @SerializedName("generationtime_ms")
    private double generationtimeMs;

    // Pola do obsługi błędów z API
    private boolean error;
    private String reason;

    // Gettery zwracające dane odpowiedzi
    public List<Location> getResults() {
        return results;
    }

    public boolean isError() {
        return error;
    }

    public String getReason() {
        return reason;
    }

    // Settery do ustawiania danych odpowiedzi (używane przez Gson)
    public void setResults(List<Location> results) {
        this.results = results;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    // Zwraca reprezentację tekstową obiektu dla celów debugowania
    @Override
    public String toString() {
        if (error) {
            return "GeocodingResponse{" +
                    "error=" + error +
                    ", reason='" + reason + '\'' +
                    '}';
        }
        return "GeocodingResponse{" +
                "results=" + results +
                ", generationtimeMs=" + generationtimeMs +
                '}';
    }
}