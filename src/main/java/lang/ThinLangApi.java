package lang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import common.Logger;
import common.Span;
import common.util;
import lang.LangProxy;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

public class ThinLangApi {
    private static final JLanguageTool langTool = new JLanguageTool(new AmericanEnglish());
    private final static Logger _logger = Logger.named("ThinLangApi");

    public static boolean isNormalWord(String word) throws IOException {
        List<RuleMatch> matches = correction_rules_subset(word);
        return matches.isEmpty();
    }

    public static List<RuleMatch> check(String text) throws IOException {
        // common.Timer.startTimer("ThinLangApi.check");
        var result = langTool.check(text);
        // common.Timer.prettyPrint("ThinLangApi.check", _logger);
        return result;
    }

    public static String rule2String(RuleMatch rule) {
        return String.format(
                "Rule{span=%s, type=%s, replacements=%s, message=%s}", Span.fromRule(rule).toString(),
                rule.getRule().getCategory().getId().toString(), rule.getSuggestedReplacements().toString(),
                rule.getMessage());
    }

    public static String rules2String(List<RuleMatch> rules) {
        var descriptiveStrings = new ArrayList<String>(rules.size());
        for (var rule : rules) {
            descriptiveStrings.add(rule2String(rule));
        }
        return "[" + String.join(", ", descriptiveStrings) + "]";
    }

    public static List<RuleMatch> correction_rules_subset(String text) throws IOException {
        List<RuleMatch> matches = check(text);
        var subset = new ArrayList<RuleMatch>();
        for (RuleMatch match : matches) {
            if (List.of("TYPOS", "SPELLING", "GRAMMAR", "TYPOGRAPHY")
                    .contains(match.getRule().getCategory().getId().toString())) {
                subset.add(match);
            }
        }
        _logger.trace(
                String.format("correction_rules_subset(text: %s)", text)
                        + " = subset("
                        + rules2String(matches) + ") = " + rules2String(subset));
        return subset;
    }

    public static Optional<RuleMatch> check_pos(Span word, String text) throws IOException {
        var check = correction_rules_subset(text);
        for (var rule : check) {
            var ruleSpan = Span.fromRule(rule);
            if (word.contain(ruleSpan) || word.intersects(ruleSpan)) {
                _logger.trace("check_pos('"
                        + word
                        + "', '"
                        + text + "') = pick("
                        + rules2String(check) + ") = " + rule2String(rule));

                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    public static Optional<List<String>> suggestionsForWord(Span word, String text) throws IOException {
        return check_pos(word, text).map(RuleMatch::getSuggestedReplacements);
    }

    public static List<String> spellWord(String word) throws IOException {
        List<RuleMatch> result = correction_rules_subset(word);
        if (result.size() > 0) {
            return filterIntentionalTypos(word,
                    result.get(0).getSuggestedReplacements());
        }
        return List.of();
    }

    public static List<String> unfilteredSuggestions(String word) throws IOException {
        List<RuleMatch> result = correction_rules_subset(word);
        if (result.size() > 1) {
            _logger.error("unfiltered Suggestions are more than one for one word '"
                    + word + "' " + rules2String(result));
        }
        if (result.size() > 0) {

            return result.get(0).getSuggestedReplacements();
        }
        return List.of();
    }

    public static boolean normal_word(String word) throws IOException {
        List<RuleMatch> result = correction_rules_subset(word);
        return result.size() == 0;
    }

    public static List<String> filterIntentionalTypos(
            String typo,
            List<String> libSuggestions) {
        return libSuggestions
                .stream()
                .filter(x -> intentionalTypo(x, typo))
                .collect(Collectors.toList());
    }

    public static Stream<String> filterIntentionalTyposStream(
            String typo,
            List<String> libSuggestions) {
        return libSuggestions
                .stream()
                .filter(x -> intentionalTypo(x, typo));
    }

    private final static int INTENTIONAL_TYPO_STR_DIST = 1;

    public static boolean upperDistanceEqualsLowerDistance(String word, String typo) {
        return util.stringMutationDistance(word.toLowerCase(), typo
                .toLowerCase()) == util.stringMutationDistance(word.toUpperCase(),
                        typo.toUpperCase());
    }

    public static boolean intentionalTypo(String word, String typo) {
        var v = util.stringMutationDistance(word, typo) <= INTENTIONAL_TYPO_STR_DIST
                && upperDistanceEqualsLowerDistance(word, typo)
                && !word.contains(" ")
                && LangProxy.sameStartAndEnd(word, typo)
                && !typo.contains(" ");
        return v;
    }
}
