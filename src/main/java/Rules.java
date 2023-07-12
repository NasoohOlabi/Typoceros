import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vavr.Tuple2;

public class Rules {

    static String ROOT_DIR = "/path/to/root/dir"; // Replace with actual root directory

    static Stream<Tuple2<String, String>> parseRules(String name) {
        InputStream inputStream = Rules.class.getClassLoader().getResourceAsStream("rules/" + name + ".tsv");
        assert inputStream != null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .map(line -> line.split("\t"))
                .filter(line -> line.length > 1)
                .map(line -> new Tuple2<>(line[0], line[1]));
    }

    static Tuple2<Pattern, String> compileFirst(Tuple2<String, String> x) {
        try {
            return new Tuple2<>(Pattern.compile(x._1), x._2);
        } catch (PatternSyntaxException e) {
            System.err.println(x);
            throw new IllegalArgumentException("compilable " + x, e);
        }
    }

    static List<Tuple2<Pattern, String>> WORD_CORRECTION_RULES = Stream.concat(
            parseRules("anti.variant"),
            Stream.concat(
                    parseRules("anti.misspelling"),
                    parseRules("anti.grammatical")))
            .map(Rules::compileFirst).collect(Collectors.toList());

    /**
     * these include the count rules and long shift rules
     */
    static List<Tuple2<Pattern, String>> KEYBOARD_CORRECTION_RULES = parseRules("anti.keyboard")
            .map(Rules::compileFirst).collect(Collectors.toList());
    /**
     * these include only fat rules
     */
    static List<Tuple2<Pattern, String>> FAT_CORRECTION_RULES = parseRules("fat.keyboard").map(Rules::compileFirst)
            .collect(Collectors.toList());

    static List<Tuple2<Pattern, String>> WORD_RULES = Stream.concat(
            parseRules("variant"),
            Stream.concat(parseRules("grammatical"), parseRules("misspelling"))).map(Rules::compileFirst)
            .collect(Collectors.toList());

    static List<Tuple2<Pattern, String>> KEYBOARD_RULES = parseRules("keyboard").map(Rules::compileFirst)
            .collect(Collectors.toList());

    static List<TypoMatch> keyboard_rules_scan(String text) {
        List<TypoMatch> matches = new ArrayList<>();
        for (var rule : Rules.KEYBOARD_RULES) {
            Pattern regex = rule._1;
            Matcher matcher = regex.matcher(text);
            while (matcher.find()) {
                addHit(text, matches, rule, regex, matcher);
            }
        }
        return matches;
    }

    static List<TypoMatch> word_rules_scan(String text) {
        List<TypoMatch> matches = new ArrayList<>();
        for (var rule : Rules.WORD_RULES) {
            Pattern regex = rule._1;
            Matcher matcher = regex.matcher(text);
            if (matcher.matches()) {
                addHit(text, matches, rule, regex, matcher);
            }
        }
        return matches;
    }

    private static void addHit(String text, List<TypoMatch> matches, Tuple2<Pattern, String> rule, Pattern regex,
            Matcher matcher) {
        int start = matcher.start();
        int end = matcher.end();
        String replacement = rule._2;
        var span = Span.of(start, end);
        var match = TypoMatch.of(text,
                LangProxy.applyMatch(text, replacement, regex, span),
                span,
                (x) -> LangProxy.applyMatch(x, replacement, regex, span));
        if (match.isPresent())
            matches.add(match.get());
    }

    static List<TypoMatch> missing_letter_scan(String text) {
        List<TypoMatch> matches = new ArrayList<>();

        Pattern regex = Pattern.compile("(?<=[^\\W])[a-z](?=[^\\W])");
        Matcher matcher = regex.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            var span = Span.of(start, end);
            var match = TypoMatch.of(text, span.notIn(text), span, span::notIn);
            if (match.isPresent())
                matches.add(
                        match.get());
        }

        return matches;
    }

    static List<TypoMatch> rules_scan(String text) {
        List<TypoMatch> result = new ArrayList<>();
        result.addAll(word_rules_scan(text));
        result.addAll(keyboard_rules_scan(text));
        result.addAll(missing_letter_scan(text));
        result.sort((a, b) -> (a.sourceSpan.start -
                b.sourceSpan.start != 0) ? a.sourceSpan.start -
                        b.sourceSpan.start
                        : a.sourceSpan.end -
                                b.sourceSpan.end);
        return result;
    }
}
