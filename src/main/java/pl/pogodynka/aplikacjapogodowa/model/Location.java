package pl.pogodynka.aplikacjapogodowa.model;

// Klasa modelowa przechowująca informacje o lokalizacji z API geokodowania
public class Location {
    // Identyfikator lokalizacji
    private int id;
    // Nazwa lokalizacji (np. nazwa miasta)
    private String name;
    // Szerokość geograficzna
    private double latitude;
    // Długość geograficzna
    private double longitude;
    // Wysokość nad poziomem morza
    private double elevation;
    // Strefa czasowa
    private String timezone;
    // Kraj lokalizacji
    private String country;

    // Gettery zwracające dane lokalizacji
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getElevation() {
        return elevation;
    }

    public String getTimezone() {
        return timezone;
    }

    // Settery do ustawiania danych lokalizacji (używane przez Gson)
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    // Zwraca reprezentację tekstową obiektu dla celów debugowania
    @Override
    public String toString() {
        return "Location{" +
                "name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", country='" + country + '\'' +
                '}';
    }
}