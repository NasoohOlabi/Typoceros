package common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * A silent common.Logger
 * if stdout is being used.
 */
public class Logger {
    private FileWriter logFile;
    private FileWriter infoFile;
    private FileWriter debugFile;
    private FileWriter errorFile;
    private FileWriter progressFile;
    private FileWriter traceFile;
    private static FileWriter masterLog;
    private static final String basePath = "./Typoceros/logs/";
    private final String filePath;

    private static final HashMap<String, Logger> _loggers = new HashMap<>();
    private boolean skipMain = false;

    public static Logger named(String name) {
        var logger = new Logger(name);
        _loggers.put(name, logger);
        return logger;
    }

    public static void closeAll() throws IOException {
        for (var l : _loggers.values()) {
            l.close();
        }
    }

    private void close() throws IOException {
        if (logFile != null)
            logFile.close();
        if (infoFile != null)
            infoFile.close();
        if (debugFile != null)
            debugFile.close();
        if (errorFile != null)
            errorFile.close();
        if (progressFile != null)
            progressFile.close();
        if (traceFile != null)
            traceFile.close();
        if (masterLog != null)
            masterLog.close();
    }

    private static FileWriter createWriter(String path) {
        File f = new File(path);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                return null;
            }
        }
        FileWriter writer;
        try {
            writer = new FileWriter(f, true);
        } catch (IOException e) {
            return null;
        }
        return writer;
    }

    private static boolean mastersCreated = false;

    private static void createMasters() {
        if (!mastersCreated) {
            masterLog = createWriter(basePath + "Typoceros.log");
            if (masterLog == null)
                Config.masterLogActive = false;
            mastersCreated = true;
        }
    }

    private Logger(String filePath) {
        this.filePath = filePath;
        // TODO: create file on write not here!!!
        createMasters();
        Config.sync();
    }

    public void _log(String message, FileWriter f) {
        _log(message, f, "\n");
    }

    public void _log(String message, FileWriter f, String end) {
        try {
            f.write(message + end);
            if (Config.masterLogActive && !skipMain)
                masterLog.write(message + end);
        } catch (IOException ignored) { // don't fail on logging
        }
    }

    public void log(Object message) {
        if (Config.logFileActive)
            _log(message.toString(), getLogFile());
    }

    public void debug(Object message) {
        if (Config.debugFileActive)
            _log(message.toString(), getDebugFile());
    }

    public void info(Object message) {
        if (Config.infoFileActive)
            _log(message.toString(), getInfoFile());
    }

    public void error(Object message) {
        _log(message.toString(), getErrorFile());
    }

    public void error(Object message, Throwable exp) {
        _log(message.toString(), getErrorFile());
        _log(exp.getMessage(), getErrorFile());
        for (var ste : exp.getStackTrace()) {
            _log(ste.toString(), getErrorFile());
        }
    }

    public void trace(Object message) {
        if (Config.traceFileActive)
            _log(message.toString(), getTraceFile());
    }

    public void progress(Object message) {
        if (Config.progressFileActive)
            _log(message.toString(), getProgressFile());
    }

    public void trace_info(Object message) {
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

    public void setSkipMain(boolean b) {
        this.skipMain = b;
    }

    public FileWriter getLogFile() {
        if (logFile == null) {
            this.logFile = createWriter(basePath + filePath + ".log");
            if (this.logFile == null)
                Config.logFileActive = false;
        }
        return this.logFile;
    }

    public FileWriter getInfoFile() {
        if (this.infoFile == null) {
            this.infoFile = createWriter(basePath + filePath + ".info");
            if (this.infoFile == null)
                Config.infoFileActive = false;
        }
        return this.infoFile;
    }

    public FileWriter getDebugFile() {
        if (this.debugFile == null) {
            this.debugFile = createWriter(basePath + filePath + ".debug");
            if (this.debugFile == null)
                Config.debugFileActive = false;
        }
        return this.debugFile;
    }

    public FileWriter getErrorFile() {
        if (this.errorFile == null) {
            this.errorFile = createWriter(basePath + filePath + ".error");
            if (this.errorFile == null)
                Config.errorFileActive = false;
        }
        return this.errorFile;
    }

    public FileWriter getProgressFile() {
        if (this.progressFile == null) {
            this.progressFile = createWriter(basePath + filePath + ".progress");
            if (this.progressFile == null)
                Config.progressFileActive = false;
        }
        return this.progressFile;
    }

    public FileWriter getTraceFile() {
        if (this.traceFile == null) {
            this.traceFile = createWriter(basePath + filePath + ".trace");
            if (this.traceFile == null)
                Config.traceFileActive = false;
        }
        return this.traceFile;
    }
}
