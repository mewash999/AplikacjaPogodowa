package pl.pogodynka.aplikacjapogodowa.model;

import java.util.Map;

// Klasa modelowa przechowująca odpowiedź z API pogodowego Open-Meteo
public class OpenMeteoResponse {
    // Szerokość geograficzna lokalizacji
    private Double latitude;
    // Długość geograficzna lokalizacji
    private Double longitude;
    // Jednostki miar dla danych godzinowych
    private Map<String, String> hourlyUnits;
    // Dane godzinowe pogodowe
    private HourlyData hourly;
    // Flaga błędu API
    private boolean error;
    // Powód błędu, jeśli wystąpił
    private String reason;

    // Domyślny konstruktor wymagany przez Gson
    public OpenMeteoResponse() {
    }

    // Gettery zwracające dane odpowiedzi
    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Map<String, String> getHourlyUnits() {
        return hourlyUnits;
    }

    public HourlyData getHourly() {
        return hourly;
    }

    public boolean isError() {
        return error;
    }

    public String getReason() {
        return reason;
    }

    // Settery do ustawiania danych odpowiedzi (używane przez Gson)
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setHourlyUnits(Map<String, String> hourlyUnits) {
        this.hourlyUnits = hourlyUnits;
    }

    public void setHourly(HourlyData hourly) {
        this.hourly = hourly;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}