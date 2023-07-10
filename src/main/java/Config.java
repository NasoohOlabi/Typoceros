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

	private final static Logger _logger = new Logger("./Typoceros/logs/Config");

	public static void sync() {
		File file = new File(config_file);
		if (!file.exists()) {
			try {
				file.createNewFile();
				FileWriter writer = new FileWriter(file);
				writer.write("span_size=" + span_size);
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
					}
				}
				reader.close();

			} catch (IOException e) {
				_logger.debug("Error reading file: " + e.getMessage());
			}
		}
	}
}
