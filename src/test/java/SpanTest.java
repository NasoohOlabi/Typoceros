import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.vavr.Tuple3;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpanTest {
    private static boolean setUpIsDone = false;

    @Before
    public void setUp() {
        if (setUpIsDone) {
            return;
        }
        Config.sync();
        setUpIsDone = true;
    }

    @Test
    public void intersectTests() {
        assertTrue(Span.of(2, 6).intersects(Span.of(2, 5)));
        assertTrue(Span.of(2, 5).intersects(Span.of(2, 6)));
    }

    @Test
    public void a() {
        assertEquals(
                4,
                util.stringMutationDistance("ARTE", "arte"));
    }

    @Test
    public void b() {
        String input = "example";

        List<Tuple3<Span, String, Pattern>> matches = new ArrayList<>();

        Pattern regex = Pattern.compile("(?<=[^\\W])[a-z](?=[^\\W])");
        Matcher matcher = regex.matcher(input);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String replacement = "";
            matches.add(new Tuple3<>(Span.of(start - 1, end + 1), replacement, regex));
        }

        System.out.println(matches);
        matches.forEach(t -> {
            var span = t._1();
            var repl = t._2();
            var re = t._3();
            var hitSection = input.substring(span.start, span.end);
            System.out.println("hitSection=(" + hitSection + ")");
            System.out.println(input.substring(0, span.start)
                    + re.matcher(hitSection).replaceAll(repl)
                    + input.substring(span.end));
        });
    }
}
