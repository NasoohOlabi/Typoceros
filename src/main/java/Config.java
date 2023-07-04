import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.slf4j.helpers.BasicMarker;

public class Config {
	public static String config_file = "./Typoceros/config/config.env";
	public static String logging_file = "./Typoceros/config/logging.env";
	public static String dict_file = "./Typoceros/config/dictionary";
	public static int span_size = 10;
	private static boolean handlers_set = false;
	public static HashMap<String, Level> log_level_map = new HashMap<String, Level>(Map.of(
			"Typoceros", Level.FINE,
			"Typoceros.App", Level.FINE,
			"Typoceros.LangProxy", Level.FINE,
			"Typoceros.StringSpans", Level.FINE,
			"Typoceros.Timer", Level.FINE,
			"Typoceros.Typo", Level.FINE,
			"Typoceros.TypoTest", Level.FINE,
			"Typoceros.Config", Level.FINE));

	private final static Logger _logger = LogManager.getLogger("Typoceros.Config");

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

	public static String levelsToString() {
		var sb = new StringBuilder();
		log_level_map.entrySet().stream().forEach((entry) -> {
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(entry.getValue());
			sb.append("\n");
		});
		return sb.toString();
	}

	public static void syncLogLevels() {
		File file = new File(logging_file);
		if (!file.exists()) {
			try {
				file.createNewFile();
				FileWriter writer = new FileWriter(file);
				writer.write(levelsToString());
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
					var value = Level.parse(parts[1]);
					log_level_map.put(key, value);
					var logger = LogManager.getLogger(key);

//					if (!handlers_set)
//						logger.addHandler(
//								new FileHandler("./Typoceros/logs/"
//										+ key.replace("Typoceros.", "") + ".log"));
				}
				handlers_set = true;
				reader.close();
			} catch (IOException e) {
				_logger.debug("Error reading file: " + e.getMessage());
			}
		}
	}

//	public static void flushLogs() {
//		for (var log : log_level_map.keySet()) {
//			for (var handler : LogManager.getLogger(log).getHandlers()) {
//				handler.flush();
//			}
//		}
//	}

}
