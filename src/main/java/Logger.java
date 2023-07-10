import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
	private final File logFile;
	private final File infoFile;
	private final File debugFile;
	private final File errorFile;
	private final File traceFile;

	public Logger(String filePath) {
		this.logFile = new File(filePath);
		this.infoFile = new File(filePath + ".info");
		this.debugFile = new File(filePath + ".debug");
		this.errorFile = new File(filePath + ".error");
		this.traceFile = new File(filePath + ".trace");
	}

	public void _log(String message, File f) {
		_log(message, f, "\n");
	}

	public void _log(String message, File f, String end) {
		try {
			if (!f.exists()) {
				f.createNewFile();
			}

			try (var writer = new FileWriter(f, true)) {
				writer.write(message + end);
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

	public void trace_info(String message) {
		_log(message, traceFile);
		_log(message, infoFile);
	}

	public void trace(String name, Object value) {
		_log(name + "=" + value.toString(), traceFile);
	}

	public void debugSeparatorStart() {
		_log("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv", debugFile);
	}

	public void debugSeparatorEnd() {
		_log("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^", debugFile);
	}

}
