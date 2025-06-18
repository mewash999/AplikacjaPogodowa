package pl.pogodynka.aplikacjapogodowa.model;

// Klasa przechowująca wpis w pamięci podręcznej dla danych pogodowych
public class CacheEntry {
    // Dane pogodowe z API Open-Meteo
    private OpenMeteoResponse data;
    // Czas utworzenia wpisu w milisekundach
    private long timestamp;
    // Flaga określająca, czy dane są prognozą, czy danymi historycznymi
    private boolean isForecast;

    // Konstruktor z danymi, domyślnie dla prognozy
    public CacheEntry(OpenMeteoResponse data) {
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.isForecast = true;
    }

    // Konstruktor z danymi i flagą typu danych
    public CacheEntry(OpenMeteoResponse data, boolean isForecast) {
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.isForecast = isForecast;
    }

    // Gettery zwracające dane wpisu
    public OpenMeteoResponse getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isForecast() {
        return isForecast;
    }

    // Settery do ustawiania danych wpisu
    public void setData(OpenMeteoResponse data) {
        this.data = data;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setForecast(boolean isForecast) {
        this.isForecast = isForecast;
    }
}