package pl.pogodynka.aplikacjapogodowa.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// Klasa modelowa przechowująca dane godzinowe pogodowe z API Open-Meteo
public class HourlyData {
    // Lista czasów dla danych godzinowych
    private List<String> time;

    // Temperatura powietrza na wysokości 2 metrów, mapowana z JSON
    @SerializedName("temperature_2m")
    private List<Double> temperature2m;

    // Prędkość wiatru na wysokości 10 metrów, mapowana z JSON
    @SerializedName("windspeed_10m")
    private List<Double> windspeed10m;

    // Opady deszczu, mapowane z JSON
    @SerializedName("precipitation")
    private List<Double> precipitation;

    // Temperatura gleby na głębokości 0 cm (dla prognoz), mapowana z JSON
    @SerializedName("soil_temperature_0cm")
    private List<Double> soilTemperature0cm;

    // Temperatura gleby na głębokości 0-7 cm (dla danych historycznych), mapowana z JSON
    @SerializedName("soil_temperature_0_to_7cm")
    private List<Double> soilTemperature0to7cm;

    // Ciśnienie atmosferyczne na poziomie powierzchni, mapowane z JSON
    @SerializedName("surface_pressure")
    private List<Double> surfacePressure;

    // Gettery zwracające dane godzinowe
    public List<String> getTime() {
        return time;
    }

    public List<Double> getTemperature2m() {
        return temperature2m;
    }

    public List<Double> getWindspeed10m() {
        return windspeed10m;
    }

    public List<Double> getPrecipitation() {
        return precipitation;
    }

    public List<Double> getSoilTemperature0cm() {
        return soilTemperature0cm;
    }

    public List<Double> getSoilTemperature0to7cm() {
        return soilTemperature0to7cm;
    }

    public List<Double> getSurfacePressure() {
        return surfacePressure;
    }

    // Settery do ustawiania danych godzinowych (używane przez Gson podczas deserializacji)
    public void setTime(List<String> time) {
        this.time = time;
    }

    public void setTemperature2m(List<Double> temperature2m) {
        this.temperature2m = temperature2m;
    }

    public void setWindspeed10m(List<Double> windspeed10m) {
        this.windspeed10m = windspeed10m;
    }

    public void setPrecipitation(List<Double> precipitation) {
        this.precipitation = precipitation;
    }

    public void setSoilTemperature0cm(List<Double> soilTemperature0cm) {
        this.soilTemperature0cm = soilTemperature0cm;
    }

    public void setSoilTemperature0to7cm(List<Double> soilTemperature0to7cm) {
        this.soilTemperature0to7cm = soilTemperature0to7cm;
    }

    public void setSurfacePressure(List<Double> surfacePressure) {
        this.surfacePressure = surfacePressure;
    }

    // Zwraca reprezentację tekstową obiektu dla celów debugowania
    @Override
    public String toString() {
        return "HourlyData{" +
                "time=" + time +
                ", temperature2m=" + temperature2m +
                ", windspeed10m=" + windspeed10m +
                ", precipitation=" + precipitation +
                ", soilTemperature0cm=" + soilTemperature0cm +
                ", soilTemperature0to7cm=" + soilTemperature0to7cm +
                ", surfacePressure=" + surfacePressure +
                '}';
    }
}