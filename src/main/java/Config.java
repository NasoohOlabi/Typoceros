import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class Config {
	public static String config_file = "config.env";
	public static String dict_file = ".dictionary";
	public static int span_size = 10;
	private final static Logger _logger = Logger.getLogger("Typoceros.log");

	public static void sync() {
		File file = new File(config_file);
		if (!file.exists()) {
			try {
				file.createNewFile();
				FileWriter writer = new FileWriter(file);
				writer.write("span_size=" + span_size);
				writer.close();
			} catch (IOException e) {
				_logger.finest("Error writing file: " + e.getMessage());
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
					}
				}
				reader.close();

			} catch (IOException e) {
				_logger.finest("Error reading file: " + e.getMessage());
			}
		}
	}

	public static List<String> dict() {
		File file = new File(dict_file);
		if (!file.exists()) {
			try (
					FileWriter writer = new FileWriter(file)) {
				writer.close();
			} catch (IOException e) {
				_logger.finest("Error writing file: " + e.getMessage());
			}
		} else {
			try (
					BufferedReader reader = new BufferedReader(new FileReader(file))) {
				var lines = reader.lines().toList();
				reader.close();
				return lines;
			} catch (Exception e) {
				_logger.finest("Error reading " + dict_file + ": " + e.getMessage());
			}
		}
		return List.of();
	}

}
