import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Logger {
    private final FileWriter logFile;
    private final FileWriter infoFile;
    private final FileWriter debugFile;
    private final FileWriter errorFile;
    private final FileWriter progressFile;
    private final FileWriter traceFile;
    private static FileWriter masterLog;
    private static final String basePath = "./Typoceros/logs/";

    private static final HashMap<String, Logger> _loggers = new HashMap<>();
    private boolean skipMain = false;

    public static Logger named(String name) {
        var logger = new Logger(name);
        _loggers.put(name, logger);
        return logger;
    }

    public static void closeAll() throws IOException {
        for (var l :
                _loggers.values()) {
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
        FileWriter writer = null;
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
        // TODO: create file on write not here!!!
        this.logFile = createWriter(basePath + filePath + ".log");
        if (this.logFile == null)
            Config.logFileActive = false;
        this.infoFile = createWriter(basePath + filePath + ".info");
        if (this.infoFile == null)
            Config.infoFileActive = false;
        this.debugFile = createWriter(basePath + filePath + ".debug");
        if (this.debugFile == null)
            Config.debugFileActive = false;
        this.errorFile = createWriter(basePath + filePath + ".error");
        if (this.errorFile == null)
            Config.errorFileActive = false;
        this.progressFile = createWriter(basePath + filePath + ".progress");
        if (this.progressFile == null)
            Config.progressFileActive = false;
        this.traceFile = createWriter(basePath + filePath + ".trace");
        if (this.traceFile == null)
            Config.traceFileActive = false;
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
            _log(message.toString(), logFile);
    }

    public void debug(Object message) {
        if (Config.debugFileActive)
            _log(message.toString(), debugFile);
    }

    public void info(Object message) {
        if (Config.infoFileActive)
            _log(message.toString(), infoFile);
    }

    public void error(Object message) {
        _log(message.toString(), errorFile);
    }

    public void error(Object message, Throwable exp) {
        _log(message.toString(), errorFile);
        _log(exp.getMessage(), errorFile);
        for (var ste : exp.getStackTrace()) {
            _log(ste.toString(), errorFile);
        }
    }

    public void trace(Object message) {
        if (Config.traceFileActive)
            _log(message.toString(), traceFile);
    }

    public void progress(Object message) {
        if (Config.progressFileActive)
            _log(message.toString(), progressFile);
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
}
