import io.vavr.Tuple2;

import java.util.HashMap;
import java.util.Map;

public class Timer {
    private static final Map<String, Long> startTimes = new HashMap<>();
    private static final Map<String, Tuple2<Long, Long>> acc = new HashMap<>();

    public static void startTimer(String methodName) {
        Timer.startTimes.put(methodName, System.currentTimeMillis());
    }

    public static long endTimer(String methodName) {
        long endTime = System.currentTimeMillis();
        long startTime = Timer.startTimes.get(methodName);
        long duration = endTime - startTime;
        acc.compute(methodName, (key, old) -> (old == null)
                ? new Tuple2<>(duration, 1L)
                : new Tuple2<>(old._1 + duration, old._2 + 1));
        return duration;
    }

    public static void prettyPrint(String methodName, Logger _logger) {
        Long elapsedTime = Timer.endTimer(methodName);
        _logger.debug(String.format("\t\tTimer took %3d ms for '%s' overall %d for %d calls averaging at %.2f per call"
                , elapsedTime
                , methodName
                ,acc.get(methodName)._1
                ,acc.get(methodName)._2
                ,(double) acc.get(methodName)._1 / acc.get(methodName)._2
        ));
    }

    public static void reset() {
        acc.clear();
        startTimes.clear();
    }
}