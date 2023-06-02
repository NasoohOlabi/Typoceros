import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Timer {
    private final static Logger _logger = Logger.getLogger("Typoceros.Timer");

    private static final Map<String, Long> startTimes = new HashMap<>();

    public static void startTimer(String tag) {
        Timer.startTimes.put(tag, System.currentTimeMillis());
    }

    public static long endTimer(String tag) {
        long endTime = System.currentTimeMillis();
        long startTime = Timer.startTimes.get(tag);
        return endTime - startTime;
    }

    public static void prettyPrint(String tag) {
        Long elapsedTime = Timer.endTimer(tag);
        _logger.finest(String.format("\t\t\tTimer took %3d ms for '%s' ", elapsedTime, tag));
    }
}