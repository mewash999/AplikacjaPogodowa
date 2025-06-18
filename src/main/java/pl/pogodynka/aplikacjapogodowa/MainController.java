package pl.pogodynka.aplikacjapogodowa;

import javafx.fxml.FXML;
import java.io.BufferedReader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.FileChooser;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import pl.pogodynka.aplikacjapogodowa.model.HourlyData;
import pl.pogodynka.aplikacjapogodowa.model.Location;
import pl.pogodynka.aplikacjapogodowa.model.GeocodingResponse;
import pl.pogodynka.aplikacjapogodowa.model.OpenMeteoResponse;
import pl.pogodynka.aplikacjapogodowa.model.CacheEntry;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

// Kontroler główny aplikacji pogodowej, odpowiadający za obsługę interfejsu użytkownika i logiki pobierania danych
public class MainController {

    // Pola oznaczone adnotacją @FXML są powiązane z elementami interfejsu zdefiniowanymi w pliku FXML
    @FXML private RadioButton cityRadio; // Przycisk radiowy do wyboru wprowadzenia miasta
    @FXML private RadioButton coordsRadio; // Przycisk radiowy do wyboru wprowadzenia współrzędnych
    @FXML private ToggleGroup locationMethodToggle; // Grupa przycisków radiowych do wyboru metody lokalizacji
    @FXML private VBox cityInputContainer; // Kontener dla pola wprowadzania nazwy miasta
    @FXML private TextField cityInputField; // Pole tekstowe do wprowadzania nazwy miasta
    @FXML private HBox coordsInputContainer; // Kontener dla pól wprowadzania współrzędnych
    @FXML private TextField latInputField; // Pole tekstowe dla szerokości geograficznej
    @FXML private TextField lonInputField; // Pole tekstowe dla długości geograficznej
    @FXML private RadioButton forecastRadio; // Przycisk radiowy do wyboru prognozy pogody
    @FXML private RadioButton historicalRadio; // Przycisk radiowy do wyboru danych historycznych
    @FXML private ToggleGroup dataTypeToggle; // Grupa przycisków radiowych do wyboru typu danych
    @FXML private HBox dateRangeContainer; // Kontener dla wyboru zakresu dat
    @FXML private DatePicker startDatePicker; // Pole wyboru daty początkowej
    @FXML private DatePicker endDatePicker; // Pole wyboru daty końcowej
    @FXML private VBox checkboxContainer; // Kontener dla checkboxów zmiennych pogodowych
    @FXML private CheckBox windspeedCheckbox; // Checkbox dla prędkości wiatru
    @FXML private CheckBox temp2mCheckbox; // Checkbox dla temperatury powietrza
    @FXML private CheckBox rainCheckbox; // Checkbox dla opadów deszczu
    @FXML private CheckBox soilTempCheckbox; // Checkbox dla temperatury gleby
    @FXML private CheckBox surfacePressureCheckbox; // Checkbox dla ciśnienia atmosferycznego
    @FXML private Button fetchDataButton; // Przycisk do pobierania danych
    @FXML private Label statusLabel; // Etykieta do wyświetlania komunikatów statusu
    @FXML private VBox forecastResultsBox; // Kontener dla wyników prognozy
    @FXML private VBox historicalResultsBox; // Kontener dla wyników danych historycznych

    // Prywatne pola do zarządzania połączeniami HTTP, serializacją JSON, pulą wątków i pamięcią podręczną
    private HttpClient httpClient; // Klient HTTP do wysyłania żądań do API
    private Gson gson; // Obiekt Gson do serializacji/deserializacji JSON
    private ExecutorService executorService; // Pula wątków do wykonywania operacji asynchronicznych
    private Location selectedLocation; // Wybrana lokalizacja (miasto lub współrzędne)
    private OpenMeteoResponse lastWeatherData; // Ostatnie pobrane dane pogodowe
    private Map<String, CacheEntry> weatherCache; // Pamięć podręczna dla danych pogodowych
    private static final String CACHE_FILE_NAME = "weather_cache.txt"; // Nazwa pliku pamięci podręcznej
    private static final long FORECAST_TTL_MS = 15 * 60 * 1000; // Czas ważności danych prognozy (15 minut)
    private static final long HISTORICAL_TTL_MS = 7 * 24 * 60 * 60 * 1000; // Czas ważności danych historycznych (7 dni)

    // Inicjalizacja kontrolera po załadowaniu pliku FXML
    @FXML
    public void initialize() {
        // Inicjalizacja klienta HTTP do komunikacji z API
        httpClient = HttpClient.newHttpClient();
        // Inicjalizacja Gson z formatowaniem pretty-printing dla czytelności JSON
        gson = new GsonBuilder().setPrettyPrinting().create();
        // Inicjalizacja puli wątków z dwoma wątkami do operacji asynchronicznych
        executorService = Executors.newFixedThreadPool(2);
        // Inicjalizacja pamięci podręcznej jako synchronizowanej mapy LinkedHashMap
        weatherCache = Collections.synchronizedMap(new LinkedHashMap<>());
        // Wczytanie pamięci podręcznej z pliku
        loadCache();

        // Dodanie słuchacza dla zmiany wyboru metody lokalizacji (miasto lub współrzędne)
        locationMethodToggle.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (cityRadio.isSelected()) {
                // Pokazanie pola wprowadzania miasta i ukrycie pól współrzędnych
                cityInputContainer.setVisible(true);
                cityInputContainer.setManaged(true);
                coordsInputContainer.setVisible(false);
                coordsInputContainer.setManaged(false);
            } else if (coordsRadio.isSelected()) {
                // Pokazanie pól współrzędnych i ukrycie pola miasta
                cityInputContainer.setVisible(false);
                cityInputContainer.setManaged(false);
                coordsInputContainer.setVisible(true);
                coordsInputContainer.setManaged(true);
            }
            // Czyszczenie stylów walidacji i ustawienie domyślnego komunikatu statusu
            clearValidationStyles();
            statusLabel.setText("Wprowadź dane i naciśnij Pobierz dane.");
            statusLabel.getStyleClass().remove("error-message");
            statusLabel.getStyleClass().add("status-message");
        });

        // Dodanie słuchacza dla zmiany typu danych (prognoza lub dane historyczne)
        dataTypeToggle.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            boolean isHistorical = historicalRadio.isSelected();
            // Pokazanie lub ukrycie kontenera z wyborem dat w zależności od typu danych
            dateRangeContainer.setVisible(isHistorical);
            dateRangeContainer.setManaged(isHistorical);
            checkboxContainer.setVisible(true);
            checkboxContainer.setManaged(true);
            clearValidationStyles();
            statusLabel.setText("Wprowadź dane i naciśnij Pobierz dane.");
            statusLabel.getStyleClass().remove("error-message");
            statusLabel.getStyleClass().add("status-message");
        });

        // Ustawienie domyślnej widoczności kontenerów przy inicjalizacji
        cityInputContainer.setVisible(true);
        cityInputContainer.setManaged(true);
        coordsInputContainer.setVisible(false);
        coordsInputContainer.setManaged(false);
        dateRangeContainer.setVisible(false);
        dateRangeContainer.setManaged(false);
        checkboxContainer.setVisible(true);
        checkboxContainer.setManaged(true);
        startDatePicker.setPromptText("");
        endDatePicker.setPromptText("");
    }

    // Zapisuje pamięć podręczną do pliku w formacie tekstowym
    private void saveCache() {
        executorService.submit(() -> {
            try (PrintWriter writer = new PrintWriter(new FileWriter(CACHE_FILE_NAME))) {
                // Iteracja po wpisach w pamięci podręcznej
                for (Map.Entry<String, CacheEntry> entry : weatherCache.entrySet()) {
                    String key = entry.getKey();
                    CacheEntry cacheEntry = entry.getValue();
                    OpenMeteoResponse data = cacheEntry.getData();
                    // Zapisanie nagłówka wpisu
                    writer.println("--- Cache Entry ---");
                    writer.println("Key: " + key);
                    writer.println("Timestamp: " + cacheEntry.getTimestamp());
                    writer.println("IsForecast: " + cacheEntry.isForecast());
                    // Zapisanie danych pogodowych
                    writer.println("Latitude: " + (data.getLatitude() != null ? data.getLatitude() : "null"));
                    writer.println("Longitude: " + (data.getLongitude() != null ? data.getLongitude() : "null"));
                    // Zapisanie jednostek godzinowych
                    Map<String, String> hourlyUnits = data.getHourlyUnits();
                    String unitsStr = hourlyUnits != null ? hourlyUnits.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.joining(",")) : "";
                    writer.println("HourlyUnits: " + unitsStr);
                    // Zapisanie danych godzinowych
                    HourlyData hourly = data.getHourly();
                    if (hourly != null) {
                        writer.println("HourlyTime: " + (hourly.getTime() != null ? String.join(",", hourly.getTime()) : ""));
                        writer.println("HourlyTemperature2m: " + (hourly.getTemperature2m() != null ? hourly.getTemperature2m().stream()
                                .map(val -> val != null ? String.format(Locale.US, "%.2f", val) : "null")
                                .collect(Collectors.joining(",")) : ""));
                        writer.println("HourlyWindspeed10m: " + (hourly.getWindspeed10m() != null ? hourly.getWindspeed10m().stream()
                                .map(val -> val != null ? String.format(Locale.US, "%.2f", val) : "null")
                                .collect(Collectors.joining(",")) : ""));
                        writer.println("HourlyPrecipitation: " + (hourly.getPrecipitation() != null ? hourly.getPrecipitation().stream()
                                .map(val -> val != null ? String.format(Locale.US, "%.2f", val) : "null")
                                .collect(Collectors.joining(",")) : ""));
                        writer.println("HourlySoilTemperature0cm: " + (hourly.getSoilTemperature0cm() != null ? hourly.getSoilTemperature0cm().stream()
                                .map(val -> val != null ? String.format(Locale.US, "%.2f", val) : "null")
                                .collect(Collectors.joining(",")) : ""));
                        writer.println("HourlySoilTemperature0to7cm: " + (hourly.getSoilTemperature0to7cm() != null ? hourly.getSoilTemperature0to7cm().stream()
                                .map(val -> val != null ? String.format(Locale.US, "%.2f", val) : "null")
                                .collect(Collectors.joining(",")) : ""));
                        writer.println("HourlySurfacePressure: " + (hourly.getSurfacePressure() != null ? hourly.getSurfacePressure().stream()
                                .map(val -> val != null ? String.format(Locale.US, "%.2f", val) : "null")
                                .collect(Collectors.joining(",")) : ""));
                    }
                }
                System.out.println("DEBUG: Cache saved successfully to " + CACHE_FILE_NAME);
            } catch (IOException e) {
                System.err.println("ERROR: Error saving cache to file: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // Wczytuje pamięć podręczną z pliku tekstowego
    private void loadCache() {
        File cacheFile = new File(CACHE_FILE_NAME);
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try (BufferedReader reader = new BufferedReader(new FileReader(CACHE_FILE_NAME))) {
                Map<String, CacheEntry> loadedCache = new LinkedHashMap<>();
                String line;
                String currentKey = null;
                long timestamp = 0;
                boolean isForecast = false;
                Double latitude = null;
                Double longitude = null;
                Map<String, String> hourlyUnits = new HashMap<>();
                HourlyData hourlyData = new HourlyData();
                // Odczyt linii pliku
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.equals("--- Cache Entry ---")) {
                        if (currentKey != null) {
                            // Zapisanie poprzedniego wpisu do pamięci podręcznej
                            OpenMeteoResponse response = new OpenMeteoResponse();
                            response.setLatitude(latitude);
                            response.setLongitude(longitude);
                            response.setHourlyUnits(hourlyUnits);
                            response.setHourly(hourlyData);
                            CacheEntry entry = new CacheEntry(response, isForecast);
                            entry.setTimestamp(timestamp);
                            loadedCache.put(currentKey, entry);
                        }
                        // Rozpoczęcie nowego wpisu
                        currentKey = null;
                        timestamp = 0;
                        isForecast = false;
                        latitude = null;
                        longitude = null;
                        hourlyUnits = new HashMap<>();
                        hourlyData = new HourlyData();
                        continue;
                    }
                    // Parsowanie poszczególnych linii wpisu
                    if (line.startsWith("Key: ")) {
                        currentKey = line.substring(5);
                    } else if (line.startsWith("Timestamp: ")) {
                        timestamp = Long.parseLong(line.substring(11));
                    } else if (line.startsWith("IsForecast: ")) {
                        isForecast = Boolean.parseBoolean(line.substring(12));
                    } else if (line.startsWith("Latitude: ")) {
                        String val = line.substring(10);
                        latitude = "null".equals(val) ? null : Double.parseDouble(val);
                    } else if (line.startsWith("Longitude: ")) {
                        String val = line.substring(11);
                        longitude = "null".equals(val) ? null : Double.parseDouble(val);
                    } else if (line.startsWith("HourlyUnits: ")) {
                        String units = line.substring(13);
                        if (!units.isEmpty()) {
                            for (String unit : units.split(",")) {
                                String[] parts = unit.split("=");
                                if (parts.length == 2) {
                                    hourlyUnits.put(parts[0], parts[1]);
                                }
                            }
                        }
                    } else if (line.startsWith("HourlyTime: ")) {
                        String times = line.substring(12);
                        hourlyData.setTime(times.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(times.split(","))));
                    } else if (line.startsWith("HourlyTemperature2m: ")) {
                        String values = line.substring(20);
                        hourlyData.setTemperature2m(values.isEmpty() ? new ArrayList<>() : parseDoubleList(values));
                    } else if (line.startsWith("HourlyWindspeed10m: ")) {
                        String values = line.substring(20);
                        hourlyData.setWindspeed10m(values.isEmpty() ? new ArrayList<>() : parseDoubleList(values));
                    } else if (line.startsWith("HourlyPrecipitation: ")) {
                        String values = line.substring(21);
                        hourlyData.setPrecipitation(values.isEmpty() ? new ArrayList<>() : parseDoubleList(values));
                    } else if (line.startsWith("HourlySoilTemperature0cm: ")) {
                        String values = line.substring(26);
                        hourlyData.setSoilTemperature0cm(values.isEmpty() ? new ArrayList<>() : parseDoubleList(values));
                    } else if (line.startsWith("HourlySoilTemperature0to7cm: ")) {
                        String values = line.substring(29);
                        hourlyData.setSoilTemperature0to7cm(values.isEmpty() ? new ArrayList<>() : parseDoubleList(values));
                    } else if (line.startsWith("HourlySurfacePressure: ")) {
                        String values = line.substring(23);
                        hourlyData.setSurfacePressure(values.isEmpty() ? new ArrayList<>() : parseDoubleList(values));
                    }
                }
                // Zapisanie ostatniego wpisu
                if (currentKey != null) {
                    OpenMeteoResponse response = new OpenMeteoResponse();
                    response.setLatitude(latitude);
                    response.setLongitude(longitude);
                    response.setHourlyUnits(hourlyUnits);
                    response.setHourly(hourlyData);
                    CacheEntry entry = new CacheEntry(response, isForecast);
                    entry.setTimestamp(timestamp);
                    loadedCache.put(currentKey, entry);
                }
                // Usunięcie przeterminowanych wpisów
                long currentTimestamp = System.currentTimeMillis();
                loadedCache.entrySet().removeIf(entry -> {
                    long ttl = entry.getValue().isForecast() ? FORECAST_TTL_MS : HISTORICAL_TTL_MS;
                    boolean isExpired = (currentTimestamp - entry.getValue().getTimestamp()) >= ttl;
                    if (isExpired) {
                        System.out.println("DEBUG: Removing expired entry with key: " + entry.getKey());
                    }
                    return isExpired;
                });
                // Aktualizacja pamięci podręcznej
                weatherCache.putAll(loadedCache);
                System.out.println("DEBUG: Cache loaded successfully from " + CACHE_FILE_NAME);
            } catch (IOException | NumberFormatException e) {
                System.err.println("ERROR: Error loading cache from file: " + e.getMessage());
                e.printStackTrace();
                weatherCache.clear();
            }
        } else {
            System.out.println("DEBUG: Cache file not found or is empty, starting with empty cache.");
        }
    }

    // Parsuje listę wartości Double z ciągu znaków oddzielonych przecinkami
    private List<Double> parseDoubleList(String values) {
        List<Double> result = new ArrayList<>();
        if (values != null && !values.isEmpty()) {
            for (String val : values.split(",")) {
                result.add("null".equals(val) ? null : Double.parseDouble(val));
            }
        }
        return result;
    }

    // Obsługuje kliknięcie przycisku "Pobierz dane"
    @FXML
    private void fetchData() {
        clearValidationStyles();
        statusLabel.setText("Pobieram dane...");
        statusLabel.getStyleClass().remove("error-message");
        statusLabel.getStyleClass().add("status-message");

        // Walidacja wprowadzonych danych
        if (!validateInputs()) {
            Platform.runLater(() -> {
                statusLabel.setText("Błąd walidacji danych. Sprawdź wprowadzone wartości.");
                statusLabel.getStyleClass().remove("status-message");
                statusLabel.getStyleClass().add("error-message");
            });
            return;
        }

        // Asynchroniczne pobieranie danych w osobnym wątku
        executorService.submit(() -> {
            try {
                double latitude;
                double longitude;
                if (cityRadio.isSelected()) {
                    // Pobieranie współrzędnych dla miasta
                    String city = cityInputField.getText().trim();
                    Location location = fetchCoordinatesForCity(city);
                    if (location == null) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Nie znaleziono miasta lub błąd geokodowania.");
                            statusLabel.getStyleClass().remove("status-message");
                            statusLabel.getStyleClass().add("error-message");
                        });
                        return;
                    }
                    selectedLocation = location;
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                } else {
                    // Pobieranie współrzędnych z pól tekstowych
                    try {
                        latitude = Double.parseDouble(latInputField.getText());
                        longitude = Double.parseDouble(lonInputField.getText());
                        selectedLocation = new Location();
                        selectedLocation.setLatitude(latitude);
                        selectedLocation.setLongitude(longitude);
                        selectedLocation.setName("Współrzędne: " + String.format(Locale.US, "%.4f", latitude) +
                                ", " + String.format(Locale.US, "%.4f", longitude));
                    } catch (NumberFormatException e) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Błąd: Szerokość i długość geograficzna muszą być liczbami.");
                            statusLabel.getStyleClass().remove("status-message");
                            statusLabel.getStyleClass().add("error-message");
                        });
                        return;
                    }
                }
                // Pobieranie danych pogodowych
                fetchWeatherData(latitude, longitude, forecastRadio.isSelected());
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Błąd połączenia!");
                    statusLabel.getStyleClass().remove("status-message");
                    statusLabel.getStyleClass().add("error-message");
                    e.printStackTrace();
                });
            }
        });
    }

    // Pobiera współrzędne geograficzne dla podanego miasta z API geokodowania
    private Location fetchCoordinatesForCity(String city) throws IOException, InterruptedException, JsonSyntaxException {
        String geocodingUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                java.net.URLEncoder.encode(city, "UTF-8") + "&count=1&language=pl&format=json";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(geocodingUrl))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            GeocodingResponse geocodingResponse = gson.fromJson(response.body(), GeocodingResponse.class);
            if (geocodingResponse != null && !geocodingResponse.isError() &&
                    geocodingResponse.getResults() != null && !geocodingResponse.getResults().isEmpty()) {
                return geocodingResponse.getResults().get(0);
            } else {
                Platform.runLater(() -> showAlert(AlertType.ERROR, "Błąd geokodowania",
                        "Nie znaleziono miasta: " + city + " lub API zwróciło błąd: " +
                                geocodingResponse.getReason()));
                return null;
            }
        } else {
            Platform.runLater(() -> showAlert(AlertType.ERROR, "Błąd sieci",
                    "Wystąpił błąd podczas pobierania danych geokodowania. Kod statusu: " +
                            response.statusCode()));
            return null;
        }
    }

    // Pobiera dane pogodowe z API Open-Meteo lub z pamięci podręcznej
    private void fetchWeatherData(double latitude, double longitude, boolean isForecast) {
        String apiUrl = buildOpenMeteoApiUrl(latitude, longitude, isForecast);
        System.out.println("DEBUG: Generated API URL: " + apiUrl);
        if (apiUrl == null) {
            System.out.println("DEBUG: API URL is null, showing error.");
            Platform.runLater(() -> {
                statusLabel.setText("Błąd: Nieprawidłowe parametry zapytania.");
                showAlert(AlertType.ERROR, "Błąd", "Nieprawidłowe parametry zapytania. Sprawdź wprowadzone dane.");
            });
            return;
        }
        // Sprawdzanie pamięci podręcznej
        String cacheKey = generateCacheKey(latitude, longitude, isForecast);
        CacheEntry cachedEntry = weatherCache.get(cacheKey);
        if (cachedEntry != null) {
            long currentTimestamp = System.currentTimeMillis();
            long ttl = isForecast ? FORECAST_TTL_MS : HISTORICAL_TTL_MS;
            long timeSinceCached = currentTimestamp - cachedEntry.getTimestamp();
            if (cachedEntry.isForecast() == isForecast && timeSinceCached < ttl) {
                System.out.println("DEBUG: Data served from cache for key: " + cacheKey +
                        " - Time to expire: " + formatRemainingTime(ttl - timeSinceCached));
                lastWeatherData = cachedEntry.getData();
                Platform.runLater(() -> {
                    if (isForecast) {
                        displayForecastData(lastWeatherData);
                    } else {
                        displayHistoricalData(lastWeatherData);
                    }
                    statusLabel.setText("Dane pobrane z cache");
                });
                return;
            } else {
                System.out.println("DEBUG: Cache entry found but not used. Type match: " + (cachedEntry.isForecast() == isForecast) +
                        ", Time since cached: " + formatRemainingTime(timeSinceCached) +
                        ", TTL: " + formatRemainingTime(ttl));
            }
        } else {
            System.out.println("DEBUG: No cache entry found for key: " + cacheKey);
        }
        // Pobieranie danych z API
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                OpenMeteoResponse weatherResponse = gson.fromJson(response.body(), OpenMeteoResponse.class);
                if (weatherResponse != null && !weatherResponse.isError()) {
                    lastWeatherData = weatherResponse;
                    weatherCache.put(cacheKey, new CacheEntry(weatherResponse, isForecast));
                    System.out.println("DEBUG: Data fetched from API and cached with key: " + cacheKey);
                    saveCache();
                    Platform.runLater(() -> {
                        if (isForecast) {
                            displayForecastData(weatherResponse);
                        } else {
                            displayHistoricalData(weatherResponse);
                        }
                        statusLabel.setText("Dane pobrane pomyślnie z API i zapisane w cache.");
                    });
                } else {
                    Platform.runLater(() -> {
                        statusLabel.setText("Błąd API Open-Meteo: " + (weatherResponse != null ? weatherResponse.getReason() : "Nieznany błąd"));
                        showAlert(AlertType.ERROR, "Błąd API", "API Open-Meteo zwróciło błąd: " +
                                (weatherResponse != null ? weatherResponse.getReason() : "Nieznany błąd"));
                    });
                }
            } else {
                Platform.runLater(() -> {
                    statusLabel.setText("Błąd HTTP: Kod " + response.statusCode());
                    showAlert(AlertType.ERROR, "Błąd HTTP", "Nie udało się pobrać danych. Kod HTTP: " + response.statusCode());
                });
            }
        } catch (java.io.IOException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Brak szczegółów błędu";
            System.out.println("DEBUG: Problem z połączeniem z API: " + errorMessage + " (Typ: " + e.getClass().getSimpleName() + ")");
            Platform.runLater(() -> {
                statusLabel.setText("Błąd: Brak połączenia z internetem lub API.");
                showAlert(AlertType.ERROR, "Brak połączenia", "Nie można połączyć się z API Open-Meteo. Sprawdź połączenie internetowe lub spróbuj ponownie później. (Szczegóły: " + errorMessage + ")");
            });
        } catch (JsonSyntaxException e) {
            System.out.println("DEBUG: JSON parsing error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                statusLabel.setText("Błąd: Nieprawidłowy format odpowiedzi API.");
                showAlert(AlertType.ERROR, "Błąd parsowania", "Nie udało się przetworzyć odpowiedzi API: " + e.getMessage());
            });
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Brak szczegółów błędu";
            System.out.println("DEBUG: Nieoczekiwany błąd: " + errorMessage + " (Typ: " + e.getClass().getSimpleName() + ")");
            e.printStackTrace();
            Platform.runLater(() -> {
                statusLabel.setText("Błąd: Wystąpił nieoczekiwany problem.");
                showAlert(AlertType.ERROR, "Wystąpił błąd", "Wystąpił nieoczekiwany błąd podczas pobierania danych: " + errorMessage);
            });
        }
    }

    // Generuje klucz dla pamięci podręcznej na podstawie parametrów
    private String generateCacheKey(double latitude, double longitude, boolean isForecast) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(isForecast ? "forecast_" : "historical_");
        keyBuilder.append(String.format(Locale.US, "%.4f", latitude)).append("_");
        keyBuilder.append(String.format(Locale.US, "%.4f", longitude)).append("_");
        List<String> selectedVariables = new ArrayList<>();
        if (windspeedCheckbox.isSelected()) selectedVariables.add("windspeed");
        if (temp2mCheckbox.isSelected()) selectedVariables.add("temperature");
        if (rainCheckbox.isSelected()) selectedVariables.add("precipitation");
        if (soilTempCheckbox.isSelected()) selectedVariables.add("soil_temperature");
        if (surfacePressureCheckbox.isSelected()) selectedVariables.add("surface_pressure");
        String sortedVariables = selectedVariables.stream().sorted().collect(Collectors.joining("-"));
        keyBuilder.append(sortedVariables).append("_");
        if (!isForecast) {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            keyBuilder.append(startDate != null ? startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "null")
                    .append("_")
                    .append(endDate != null ? endDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "null");
        }
        return keyBuilder.toString();
    }

    // Formatuje czas w milisekundach do czytelnego formatu (dni, godziny, minuty, sekundy)
    private String formatRemainingTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        long hours = minutes / 60;
        minutes %= 60;
        long days = hours / 24;
        hours %= 24;
        if (days > 0) {
            return String.format("%d dni, %d godz.", days, hours);
        } else if (hours > 0) {
            return String.format("%d godz., %d min.", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d min., %d sek.", minutes, seconds);
        } else {
            return String.format("%d sek.", seconds);
        }
    }

    // Buduje URL do API Open-Meteo na podstawie parametrów
    private String buildOpenMeteoApiUrl(double latitude, double longitude, boolean isForecast) {
        StringBuilder urlBuilder = new StringBuilder();
        if (isForecast) {
            urlBuilder.append("https://api.open-meteo.com/v1/forecast?");
            urlBuilder.append("latitude=").append(String.format(Locale.US, "%.4f", latitude));
            urlBuilder.append("&longitude=").append(String.format(Locale.US, "%.4f", longitude));
            urlBuilder.append("&current_weather=true");
            urlBuilder.append("&forecast_days=16");
            List<String> hourlyVariables = new ArrayList<>();
            if (windspeedCheckbox.isSelected()) hourlyVariables.add("windspeed_10m");
            if (temp2mCheckbox.isSelected()) hourlyVariables.add("temperature_2m");
            if (rainCheckbox.isSelected()) hourlyVariables.add("precipitation");
            if (soilTempCheckbox.isSelected()) hourlyVariables.add("soil_temperature_0cm");
            if (surfacePressureCheckbox.isSelected()) hourlyVariables.add("surface_pressure");
            if (!hourlyVariables.isEmpty()) {
                urlBuilder.append("&hourly=").append(String.join(",", hourlyVariables));
            }
            urlBuilder.append("&timezone=auto");
        } else {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            if (startDate == null || endDate == null) {
                return null;
            }
            urlBuilder.append("https://archive-api.open-meteo.com/v1/archive?");
            urlBuilder.append("latitude=").append(String.format(Locale.US, "%.4f", latitude));
            urlBuilder.append("&longitude=").append(String.format(Locale.US, "%.4f", longitude));
            urlBuilder.append("&start_date=").append(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            urlBuilder.append("&end_date=").append(endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            List<String> hourlyVariables = new ArrayList<>();
            if (windspeedCheckbox.isSelected()) hourlyVariables.add("windspeed_10m");
            if (temp2mCheckbox.isSelected()) hourlyVariables.add("temperature_2m");
            if (rainCheckbox.isSelected()) hourlyVariables.add("precipitation");
            if (soilTempCheckbox.isSelected()) hourlyVariables.add("soil_temperature_0_to_7cm");
            if (surfacePressureCheckbox.isSelected()) hourlyVariables.add("surface_pressure");
            if (!hourlyVariables.isEmpty()) {
                urlBuilder.append("&hourly=").append(String.join(",", hourlyVariables));
            }
            urlBuilder.append("&timezone=auto");
        }
        return urlBuilder.toString();
    }

    // Wyświetla dane prognozy w interfejsie użytkownika
    private void displayForecastData(OpenMeteoResponse response) {
        forecastResultsBox.setVisible(true);
        forecastResultsBox.setManaged(true);
        historicalResultsBox.setVisible(false);
        historicalResultsBox.setManaged(false);
    }

    // Wyświetla dane historyczne w interfejsie użytkownika
    private void displayHistoricalData(OpenMeteoResponse response) {
        historicalResultsBox.setVisible(true);
        historicalResultsBox.setManaged(true);
        forecastResultsBox.setVisible(false);
        forecastResultsBox.setManaged(false);
        HourlyData hourlyData = response.getHourly();
        boolean anyVariableSelected = windspeedCheckbox.isSelected() || temp2mCheckbox.isSelected() ||
                rainCheckbox.isSelected() || soilTempCheckbox.isSelected() ||
                surfacePressureCheckbox.isSelected();
        boolean hasHourlyDataAvailable = (hourlyData != null && hourlyData.getTime() != null &&
                !hourlyData.getTime().isEmpty());
        if (!anyVariableSelected || !hasHourlyDataAvailable) {
            System.out.println("DEBUG: No data available for historical display. Showing alert.");
            showAlert(AlertType.INFORMATION, "Brak danych",
                    "Brak dostępnych danych godzinowych dla wybranych zmiennych w podanym zakresie czasowym. " +
                            "Upewnij się, że wybrano zmienne i są dostępne dane w danym zakresie.");
        }
    }

    // Formatuje ciąg daty i czasu do wyświetlenia
    private String formatDateTime(String dateTimeString) {
        try {
            return java.time.LocalDateTime.parse(dateTimeString).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            try {
                return java.time.LocalDate.parse(dateTimeString).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception ex) {
                return dateTimeString;
            }
        }
    }

    // Waliduje wprowadzone dane przez użytkownika
    private boolean validateInputs() {
        boolean isValid = true;
        statusLabel.getStyleClass().remove("error-message");
        statusLabel.getStyleClass().add("status-message");
        statusLabel.setText("Wprowadź dane i naciśnij Pobierz dane.");
        clearValidationStyles();
        if (cityRadio.isSelected()) {
            if (cityInputField.getText().trim().isEmpty()) {
                addErrorStyle(cityInputField);
                isValid = false;
            }
        } else {
            if (latInputField.getText().trim().isEmpty() || !isNumeric(latInputField.getText())) {
                addErrorStyle(latInputField);
                isValid = false;
            }
            if (lonInputField.getText().trim().isEmpty() || !isNumeric(lonInputField.getText())) {
                addErrorStyle(lonInputField);
                isValid = false;
            }
        }
        if (historicalRadio.isSelected()) {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            if (startDate == null) {
                addErrorStyle(startDatePicker);
                isValid = false;
            }
            if (endDate == null) {
                addErrorStyle(endDatePicker);
                isValid = false;
            }
            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                addErrorStyle(startDatePicker);
                addErrorStyle(endDatePicker);
                showAlert(AlertType.WARNING, "Niepoprawna data", "Data początkowa nie może być późniejsza niż data końcowa.");
                isValid = false;
            }
        }
        if (!windspeedCheckbox.isSelected() && !temp2mCheckbox.isSelected() && !rainCheckbox.isSelected() &&
                !soilTempCheckbox.isSelected() && !surfacePressureCheckbox.isSelected()) {
            showAlert(AlertType.WARNING, "Wybierz zmienne", "Musisz wybrać co najmniej jedną zmienną do wizualizacji.");
            isValid = false;
        }
        return isValid;
    }

    // Sprawdza, czy ciąg jest liczbą
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Dodaje styl błędu do kontrolki
    private void addErrorStyle(javafx.scene.control.Control control) {
        control.getStyleClass().add("error");
    }

    // Czyści style walidacji z kontrolek
    private void clearValidationStyles() {
        cityInputField.getStyleClass().remove("error");
        latInputField.getStyleClass().remove("error");
        lonInputField.getStyleClass().remove("error");
        startDatePicker.getStyleClass().remove("error");
        endDatePicker.getStyleClass().remove("error");
    }

    // Wyświetla okno dialogowe z komunikatem
    private void showAlert(AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Eksportuje dane pogodowe do pliku tekstowego
    @FXML
    private void exportChartData() {
        if (lastWeatherData == null || (!forecastRadio.isSelected() && !historicalRadio.isSelected())) {
            showAlert(AlertType.INFORMATION, "Brak danych", "Nie ma danych do wyeksportowania. Pobierz je najpierw.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz dane wykresu");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT Files", "*.txt"));
        Stage stage = (Stage) fetchDataButton.getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                if (forecastRadio.isSelected()) {
                    exportForecastData(writer);
                } else if (historicalRadio.isSelected()) {
                    exportHistoricalData(writer);
                }
                showAlert(AlertType.INFORMATION, "Eksport zakończony",
                        "Dane zostały pomyślnie wyeksportowane do pliku:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showAlert(AlertType.ERROR, "Błąd eksportu",
                        "Wystąpił błąd podczas zapisywania pliku:\n" + e.getMessage());
                System.err.println("Błąd podczas eksportu danych wykresu: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Eksportuje dane prognozy do pliku
    private void exportForecastData(PrintWriter writer) {
        HourlyData hourlyData = lastWeatherData.getHourly();
        Map<String, String> hourlyUnits = lastWeatherData.getHourlyUnits();
        if (hourlyData != null && hourlyData.getTime() != null && !hourlyData.getTime().isEmpty()) {
            writer.println("--- Forecast Hourly Data ---");
            StringBuilder hourlyHeader = new StringBuilder("DateTime");
            if (temp2mCheckbox.isSelected()) hourlyHeader.append(",Temperature_2m");
            if (windspeedCheckbox.isSelected()) hourlyHeader.append(",Windspeed_10m");
            if (rainCheckbox.isSelected()) hourlyHeader.append(",Precipitation");
            if (soilTempCheckbox.isSelected()) hourlyHeader.append(",Soil_Temperature_0cm");
            if (surfacePressureCheckbox.isSelected()) hourlyHeader.append(",Surface_Pressure");
            writer.println(hourlyHeader.toString());
            for (int i = 0; i < hourlyData.getTime().size(); i++) {
                StringBuilder row = new StringBuilder(hourlyData.getTime().get(i).replace("T", " "));
                if (temp2mCheckbox.isSelected()) {
                    Double val = hourlyData.getTemperature2m() != null && i < hourlyData.getTemperature2m().size() ?
                            hourlyData.getTemperature2m().get(i) : null;
                    row.append(",").append(val != null ? String.format(Locale.US, "%.2f", val) : "N/A");
                }
                if (windspeedCheckbox.isSelected()) {
                    Double val = hourlyData.getWindspeed10m() != null && i < hourlyData.getWindspeed10m().size() ?
                            hourlyData.getWindspeed10m().get(i) : null;
                    row.append(",").append(val != null ? String.format(Locale.US, "%.2f", val) : "N/A");
                }
                if (rainCheckbox.isSelected()) {
                    Double val = hourlyData.getPrecipitation() != null && i < hourlyData.getPrecipitation().size() ?
                            hourlyData.getPrecipitation().get(i) : null;
                    row.append(",").append(val != null ? String.format(Locale.US, "%.2f", val) : "N/A");
                }
                if (soilTempCheckbox.isSelected()) {
                    Double val = hourlyData.getSoilTemperature0cm() != null && i < hourlyData.getSoilTemperature0cm().size() ?
                            hourlyData.getSoilTemperature0cm().get(i) : null;
                    row.append(",").append(val != null ? String.format(Locale.US, "%.2f", val) : "N/A");
                }
                if (surfacePressureCheckbox.isSelected()) {
                    Double val = hourlyData.getSurfacePressure() != null && i < hourlyData.getSurfacePressure().size() ?
                            hourlyData.getSurfacePressure().get(i) : null;
                    row.append(",").append(val != null ? String.format(Locale.US, "%.2f", val) : "N/A");
                }
                writer.println(row.toString());
            }
        } else {
            writer.println("No forecast hourly data selected or available.");
        }
    }

    // Eksportuje dane historyczne do pliku
    private void exportHistoricalData(PrintWriter writer) {
        HourlyData hourlyData = lastWeatherData.getHourly();
        Map<String, String> hourlyUnits = lastWeatherData.getHourlyUnits();
        if (hourlyData != null && hourlyData.getTime() != null && !hourlyData.getTime().isEmpty()) {
            writer.println("--- Historical Hourly Data ---");
            StringBuilder hourlyHeader = new StringBuilder("DateTime");
            if (temp2mCheckbox.isSelected()) hourlyHeader.append(",Temperature_2m");
            if (windspeedCheckbox.isSelected()) hourlyHeader.append(",Windspeed_10m");
            if (rainCheckbox.isSelected()) hourlyHeader.append(",Precipitation");
            if (soilTempCheckbox.isSelected()) hourlyHeader.append(",Soil_Temperature_0_to_7cm");
            if (surfacePressureCheckbox.isSelected()) hourlyHeader.append(",Surface_Pressure");
            writer.println(hourlyHeader.toString());
            for (int i = 0; i < hourlyData.getTime().size(); i++) {
                StringBuilder row = new StringBuilder(hourlyData.getTime().get(i).replace("T", " "));
                if (temp2mCheckbox.isSelected()) {
                    Double val = hourlyData.getTemperature2m() != null && i < hourlyData.getTemperature2m().size() ?
                            hourlyData.getTemperature2m().get(i) : null;
                    row.append(",").append(val != null ? String.format(Locale.US, "%.2f", val) : "N/A");
                }
                if (windspeedCheckbox.isSelected()) {
                    Double val = hourlyData.getWindspeed10m() != null && i < hourlyData.getWindspeed10m().size() ?
                            hourlyData.getWindspeed10m().get(i) : null;
                    row.append(",").append(val != null ? String.format(Locale.US, "%.2f", val) : "N/A");
                }
                if (rainCheckbox.isSelected()) {
                    Double val = hourlyData.getPrecipitation() != null && i < hourlyData.getPrecipitation().size() ?
                            hourlyData.getPrecipitation().get(i) : null;
                    row.append(",").append(val != null ? String.format(Locale.US, "%.2f", val) : "N/A");
                }
                if (soilTempCheckbox.isSelected()) {
                    Double val = hourlyData.getSoilTemperature0to7cm() != null && i < hourlyData.getSoilTemperature0to7cm().size() ?
                            hourlyData.getSoilTemperature0to7cm().get(i) : null;
                    row.append(",").append(val != null ? String.format(Locale.US, "%.2f", val) : "N/A");
                }
                if (surfacePressureCheckbox.isSelected()) {
                    Double val = hourlyData.getSurfacePressure() != null && i < hourlyData.getSurfacePressure().size() ?
                            hourlyData.getSurfacePressure().get(i) : null;
                    row.append(",").append(val != null ? String.format(Locale.US, "%.2f", val) : "N/A");
                }
                writer.println(row.toString());
            }
        } else {
            writer.println("No historical hourly data selected or available.");
        }
    }

    // Wyświetla wykres danych pogodowych w nowym oknie
    @FXML
    private void showChart() {
        if (lastWeatherData == null || (!forecastRadio.isSelected() && !historicalRadio.isSelected())) {
            showAlert(AlertType.INFORMATION, "Brak danych", "Brak danych do wyświetlenia na wykresie. Pobierz je najpierw.");
            return;
        }
        Stage chartStage = new Stage();
        chartStage.setTitle("Wykres danych pogodowych");
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Wartość");
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Dane pogodowe dla " + (selectedLocation != null ? selectedLocation.getName() : "wybranej lokalizacji"));
        lineChart.setCreateSymbols(false);
        boolean anyDataAdded = false;
        if (historicalRadio.isSelected()) {
            chartStage.setTitle("Wykres danych historycznych");
            HourlyData hourlyData = lastWeatherData.getHourly();
            Map<String, String> hourlyUnits = lastWeatherData.getHourlyUnits();
            if (hourlyData != null && hourlyData.getTime() != null && !hourlyData.getTime().isEmpty()) {
                List<String> hourlyTimes = hourlyData.getTime();
                if (temp2mCheckbox.isSelected() && hourlyData.getTemperature2m() != null) {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Temperatura powietrza (" + (hourlyUnits != null ? hourlyUnits.getOrDefault("temperature_2m", "°C") : "°C") + ")");
                    for (int i = 0; i < hourlyTimes.size(); i++) {
                        Double val = hourlyData.getTemperature2m().get(i);
                        if (val != null) {
                            series.getData().add(new XYChart.Data<>(formatDateTimeForChart(hourlyTimes.get(i)), val));
                            anyDataAdded = true;
                        }
                    }
                    lineChart.getData().add(series);
                }
                if (windspeedCheckbox.isSelected() && hourlyData.getWindspeed10m() != null) {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Prędkość wiatru (" + (hourlyUnits != null ? hourlyUnits.getOrDefault("windspeed_10m", "m/s") : "m/s") + ")");
                    for (int i = 0; i < hourlyTimes.size(); i++) {
                        Double val = hourlyData.getWindspeed10m().get(i);
                        if (val != null) {
                            series.getData().add(new XYChart.Data<>(formatDateTimeForChart(hourlyTimes.get(i)), val));
                            anyDataAdded = true;
                        }
                    }
                    lineChart.getData().add(series);
                }
                if (rainCheckbox.isSelected() && hourlyData.getPrecipitation() != null) {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Opady deszczu (" + (hourlyUnits != null ? hourlyUnits.getOrDefault("precipitation", "mm") : "mm") + ")");
                    for (int i = 0; i < hourlyTimes.size(); i++) {
                        Double val = hourlyData.getPrecipitation().get(i);
                        if (val != null) {
                            series.getData().add(new XYChart.Data<>(formatDateTimeForChart(hourlyTimes.get(i)), val));
                            anyDataAdded = true;
                        }
                    }
                    lineChart.getData().add(series);
                }
                if (soilTempCheckbox.isSelected() && hourlyData.getSoilTemperature0to7cm() != null) {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Temperatura gleby (" + (hourlyUnits != null ? hourlyUnits.getOrDefault("soil_temperature_0_to_7cm", "°C") : "°C") + ")");
                    for (int i = 0; i < hourlyTimes.size(); i++) {
                        Double val = hourlyData.getSoilTemperature0to7cm().get(i);
                        if (val != null) {
                            series.getData().add(new XYChart.Data<>(formatDateTimeForChart(hourlyTimes.get(i)), val));
                            anyDataAdded = true;
                        }
                    }
                    lineChart.getData().add(series);
                }
                if (surfacePressureCheckbox.isSelected() && hourlyData.getSurfacePressure() != null) {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Ciśnienie atmosferyczne (" + (hourlyUnits != null ? hourlyUnits.getOrDefault("surface_pressure", "hPa") : "hPa") + ")");
                    for (int i = 0; i < hourlyTimes.size(); i++) {
                        Double val = hourlyData.getSurfacePressure().get(i);
                        if (val != null) {
                            series.getData().add(new XYChart.Data<>(formatDateTimeForChart(hourlyTimes.get(i)), val));
                            anyDataAdded = true;
                        }
                    }
                    lineChart.getData().add(series);
                }
            }
        } else if (forecastRadio.isSelected()) {
            chartStage.setTitle("Wykres prognozy na 16 dni");
            HourlyData hourlyData = lastWeatherData.getHourly();
            Map<String, String> hourlyUnits = lastWeatherData.getHourlyUnits();
            if (hourlyData != null && hourlyData.getTime() != null && !hourlyData.getTime().isEmpty()) {
                List<String> hourlyTimes = hourlyData.getTime();
                if (temp2mCheckbox.isSelected() && hourlyData.getTemperature2m() != null) {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Temperatura powietrza (" + (hourlyUnits != null ? hourlyUnits.getOrDefault("temperature_2m", "°C") : "°C") + ")");
                    for (int i = 0; i < hourlyTimes.size(); i++) {
                        Double val = hourlyData.getTemperature2m().get(i);
                        if (val != null) {
                            series.getData().add(new XYChart.Data<>(formatDateTimeForChart(hourlyTimes.get(i)), val));
                            anyDataAdded = true;
                        }
                    }
                    lineChart.getData().add(series);
                }
                if (windspeedCheckbox.isSelected() && hourlyData.getWindspeed10m() != null) {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Prędkość wiatru (" + (hourlyUnits != null ? hourlyUnits.getOrDefault("windspeed_10m", "m/s") : "m/s") + ")");
                    for (int i = 0; i < hourlyTimes.size(); i++) {
                        Double val = hourlyData.getWindspeed10m().get(i);
                        if (val != null) {
                            series.getData().add(new XYChart.Data<>(formatDateTimeForChart(hourlyTimes.get(i)), val));
                            anyDataAdded = true;
                        }
                    }
                    lineChart.getData().add(series);
                }
                if (rainCheckbox.isSelected() && hourlyData.getPrecipitation() != null) {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Opady deszczu (" + (hourlyUnits != null ? hourlyUnits.getOrDefault("precipitation", "mm") : "mm") + ")");
                    for (int i = 0; i < hourlyTimes.size(); i++) {
                        Double val = hourlyData.getPrecipitation().get(i);
                        if (val != null) {
                            series.getData().add(new XYChart.Data<>(formatDateTimeForChart(hourlyTimes.get(i)), val));
                            anyDataAdded = true;
                        }
                    }
                    lineChart.getData().add(series);
                }
                if (soilTempCheckbox.isSelected() && hourlyData.getSoilTemperature0cm() != null) {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Temperatura gleby (" + (hourlyUnits != null ? hourlyUnits.getOrDefault("soil_temperature_0cm", "°C") : "°C") + ")");
                    for (int i = 0; i < hourlyTimes.size(); i++) {
                        Double val = hourlyData.getSoilTemperature0cm().get(i);
                        if (val != null) {
                            series.getData().add(new XYChart.Data<>(formatDateTimeForChart(hourlyTimes.get(i)), val));
                            anyDataAdded = true;
                        }
                    }
                    lineChart.getData().add(series);
                }
                if (surfacePressureCheckbox.isSelected() && hourlyData.getSurfacePressure() != null) {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Ciśnienie atmosferyczne (" + (hourlyUnits != null ? hourlyUnits.getOrDefault("surface_pressure", "hPa") : "hPa") + ")");
                    for (int i = 0; i < hourlyTimes.size(); i++) {
                        Double val = hourlyData.getSurfacePressure().get(i);
                        if (val != null) {
                            series.getData().add(new XYChart.Data<>(formatDateTimeForChart(hourlyTimes.get(i)), val));
                            anyDataAdded = true;
                        }
                    }
                    lineChart.getData().add(series);
                }
            }
        }
        if (!anyDataAdded) {
            showAlert(AlertType.INFORMATION, "Brak danych wykresu",
                    "Brak wybranych lub dostępnych danych do wyświetlenia na wykresie.");
            return;
        }
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(lineChart);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        Scene scene = new Scene(scrollPane, 1020, 800);
        chartStage.setScene(scene);
        chartStage.show();
    }

    // Formatuje datę i czas dla wykresu
    private String formatDateTimeForChart(String dateTimeString) {
        return dateTimeString.replace("T", " ");
    }

    // Zamyka pulę wątków i zapisuje pamięć podręczną
    public void shutdown() {
        saveCache();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}