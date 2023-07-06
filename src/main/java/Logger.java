import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
	private File logFile;
	private File infoFile;
	private File debugFile;
	private File errorFile;
	private File traceFile;

	public Logger(String filePath) {
		this.logFile = new File(filePath);
		this.infoFile = new File(filePath + ".info");
		this.debugFile = new File(filePath + ".debug");
		this.errorFile = new File(filePath + ".error");
		this.traceFile = new File(filePath + ".trace");
	}

	public void _log(String message, File f) {
		try {
			if (!f.exists()) {
				f.createNewFile();
			}

			try (var writer = new FileWriter(f, true)) {
				writer.write(message + "\n");
			}
		} catch (IOException e) {
			System.out.println("An error occurred while writing to the log file: " + e.getMessage());
		}
	}

	public void log(String message) {
		_log(message, logFile);
	}

	public void debug(String message) {
		_log(message, debugFile);
	}

	public void info(String message) {
		_log(message, infoFile);
	}

	public void error(String message) {
		_log(message, errorFile);
	}

	public void error(String message, Throwable exp) {
		_log(message, errorFile);
		_log(exp.getMessage(), errorFile);
		for (var ste : exp.getStackTrace()) {
			_log(ste.toString(), errorFile);
		}
	}

	public void trace(String message) {
		_log(message, traceFile);
	}

}
