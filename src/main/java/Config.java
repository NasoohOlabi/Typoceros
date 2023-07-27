import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Config {
    public static String config_file = "./Typoceros/config/config.env";
    public static String logging_file = "./Typoceros/config/logging.env";
    public static String dict_file = "./Typoceros/config/dictionary";
    public static int span_size = 10;
    public static boolean logFileActive = false;
    public static boolean infoFileActive = false;
    public static boolean debugFileActive = false;
    public static boolean errorFileActive = false;
    public static boolean progressFileActive = false;
    public static boolean traceFileActive = false;
    public static boolean masterLogActive = false;
    public static double min_winning_percentage = 0.5;
    public static int max_population = 23;

    private final static Logger _logger = Logger.named("Config");

    public static void sync() {
        // Create a File object representing the directory you want to create
        new File("./Typoceros").mkdirs();
        new File("./Typoceros/config").mkdirs();
        new File("./Typoceros/logs").mkdirs();
        File file = new File(config_file);
        if (!file.exists()) {
            try {
                file.createNewFile();
                FileWriter writer = new FileWriter(file);
                writer.write("span_size=" + span_size + "\n");
                writer.write("logFileActive=" + logFileActive + "\n");
                writer.write("infoFileActive=" + infoFileActive + "\n");
                writer.write("debugFileActive=" + debugFileActive + "\n");
                writer.write("errorFileActive=" + errorFileActive + "\n");
                writer.write("progressFileActive=" + progressFileActive + "\n");
                writer.write("traceFileActive=" + traceFileActive + "\n");
                writer.write("masterLogActive=" + masterLogActive + "\n");
                writer.write("min_winning_percentage=" + min_winning_percentage + "\n");
                writer.write("max_population=" + max_population + "\n");
                writer.close();
            } catch (IOException e) {
                _logger.debug("Error writing file: " + e.getMessage());
            }
        } else {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=");
                    String key = parts[0];
                    var value = parts[1];
                    // Do something with the key-value pair
                    if ("span_size".equals(key)) {
                        span_size = Integer.parseInt(value);
                    } else if ("logFileActive".equals(key)) {
                        logFileActive = "true".equals(value);
                    } else if ("infoFileActive".equals(key)) {
                        infoFileActive = "true".equals(value);
                    } else if ("debugFileActive".equals(key)) {
                        debugFileActive = "true".equals(value);
                    } else if ("errorFileActive".equals(key)) {
                        errorFileActive = "true".equals(value);
                    } else if ("progressFileActive".equals(key)) {
                        progressFileActive = "true".equals(value);
                    } else if ("traceFileActive".equals(key)) {
                        traceFileActive = "true".equals(value);
                    } else if ("masterLogActive".equals(key)) {
                        masterLogActive = "true".equals(value);
                    } else if ("min_winning_percentage".equals(key)) {
                        min_winning_percentage = Double.parseDouble(value);
                    } else if ("max_population".equals(key)) {
                        max_population = Integer.parseInt(value);
                    }
                }
                reader.close();

            } catch (IOException e) {
                _logger.debug("Error reading file: " + e.getMessage());
            }
        }
    }
}
