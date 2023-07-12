import java.util.HashMap;
import java.util.Map;

public class Timer {
    private static final Map<String, Long> startTimes = new HashMap<>();

    public static void startTimer(String tag) {
        Timer.startTimes.put(tag, System.currentTimeMillis());
    }

    public static long endTimer(String tag) {
        long endTime = System.currentTimeMillis();
        long startTime = Timer.startTimes.get(tag);
        return endTime - startTime;
    }

    public static void prettyPrint(String tag, Logger _logger) {
        Long elapsedTime = Timer.endTimer(tag);
        _logger.debug(String.format("\t\tTimer took %3d ms for '%s' ", elapsedTime, tag));
    }
}