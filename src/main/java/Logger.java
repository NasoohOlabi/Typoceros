import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
	private final File logFile;
	private final File infoFile;
	private final File debugFile;
	private final File errorFile;
	private final File progressFile;
	private final File traceFile;
	private final File masterLog;
	private final boolean logFileActive = true;
	private final boolean infoFileActive = true;
	private final boolean debugFileActive = true;
	private final boolean errorFileActive = true;
	private final boolean progressFileActive = true;
	private final boolean traceFileActive = true;
	private final boolean masterLogActive = true;
	private final String basePath = "./Typoceros/logs/";


	public Logger(String filePath) {
		this.logFile = new File(basePath + filePath + ".log");
		this.infoFile = new File(basePath + filePath + ".info");
		this.debugFile = new File(basePath + filePath + ".debug");
		this.errorFile = new File(basePath + filePath + ".error");
		this.progressFile = new File(basePath + filePath + ".progress");
		this.traceFile = new File(basePath + filePath + ".trace");
		this.masterLog = new File(basePath + "Typoceros.log");
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
			if (masterLogActive)
			try (var writer = new FileWriter(masterLog, true)) {
				writer.write(message + end);
			}
		} catch (IOException e) {
			System.out.println("An error occurred while writing to the log file: " + e.getMessage());
		}
	}

	public void log(Object message) {
		if (logFileActive)
		_log(message.toString(), logFile);
	}

	public void debug(Object message) {
		if (debugFileActive)_log(message.toString(), debugFile);
	}

	public void info(Object message) {
		if (infoFileActive)_log(message.toString(), infoFile);
	}

	public void error(Object message) {
		_log(message.toString(), errorFile);
	}

	public void error(String message, Throwable exp) {
		_log(message, errorFile);
		_log(exp.getMessage(), errorFile);
		for (var ste : exp.getStackTrace()) {
			_log(ste.toString(), errorFile);
		}
	}

	public void trace(String message) {
		if (traceFileActive)_log(message, traceFile);
	}
	public void progress(String message) {
		if (progressFileActive)_log(message, progressFile);
	}

	public void trace_info(String message) {
		trace(message);
		info(message);
	}

	public void trace(String name, Object value) {
		trace(name + "=" + value.toString());
	}

	public void traceSeparatorStart() {
		trace("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
	}

	public void traceSeparatorEnd() {
		trace("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
	}

	public void debugSeparatorStart() {
		debug("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
	}

	public void debugSeparatorEnd() {
		debug("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
	}

	public void trace_progress(String message) {
		trace(message);
		progress(message);
	}

}
