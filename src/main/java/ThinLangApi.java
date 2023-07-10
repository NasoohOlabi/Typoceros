import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

public class ThinLangApi {
	private static final JLanguageTool langTool = new JLanguageTool(new AmericanEnglish());
	private final static Logger _logger = new Logger("./Typoceros/logs/ThinLangApi");

	public static boolean isNormalWord(String word) throws IOException {
		List<RuleMatch> matches = check(word);
		return matches.isEmpty();
	}

	public static List<RuleMatch> check(String text) throws IOException {
		return langTool.check(text);
	}

	public static String correct(String text) throws IOException {
		var a = check(text);
		for (RuleMatch ruleMatch : a) {
			var span = Span.fromRule(ruleMatch);
			if (ruleMatch.getSuggestedReplacements().size() > 0)
				text = span.swap(text, ruleMatch.getSuggestedReplacements().get(0));
		}
		return text;
	}

	public static List<RuleMatch> correction_rules_subset(String text) throws IOException {
		_logger.trace("------------------------------------------");
		_logger.trace(String.format("correction_rules_subset(text: %s)", text));
		List<RuleMatch> matches = check(text);
		_logger.trace("matches", matches);
		var subset = new ArrayList<RuleMatch>();
		for (RuleMatch match : matches) {
			if (List.of("TYPOS", "SPELLING", "GRAMMAR", "TYPOGRAPHY")
					.contains(match.getRule().getCategory().getId().toString())) {
				subset.add(match);
			} else {
				_logger.trace("Discarded" + match.getRule().getCategory().getId() + " match=" + match);
			}
		}
		_logger.trace("subset", subset);
		_logger.trace("------------------------------------------");
		return subset;
	}

	public static Optional<RuleMatch> check_pos(Span word, String text) throws IOException {
		var check = check(text);
		_logger.trace("check result('" + word + "', '" + text + "'): " + check);
		for (var rule : check) {
			var ruleSpan = Span.fromRule(rule);
			if (word.contain(ruleSpan) || word.intersects(ruleSpan)) {
				return Optional.of(rule);
			} else {
				_logger.trace(
						"rule not related to word " + word + " Rule " + ruleSpan);
				_logger.trace(rule.toString());
			}
		}
		return Optional.empty();
	}

	public static List<String> spellWord(String word) throws IOException {
		List<RuleMatch> result = check(word);
		if (result.size() > 0) {
			return filterIntentionalTypos(word,
					result.get(0).getSuggestedReplacements());
		}
		return List.of();
	}

	public static boolean normal_word(String word) throws IOException {
		List<RuleMatch> result = check(word);
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
		return util.stringMutationDistance(word.toLowerCase(), typo.toLowerCase()) == util.stringMutationDistance(word,
				typo);
	}

	public static boolean intentionalTypo(String word, String typo) {
		return util.stringMutationDistance(word, typo) <= INTENTIONAL_TYPO_STR_DIST
				&& (!upperDistanceEqualsLowerDistance(word, typo)
						|| !word.substring(1, 2).equals(typo.substring(1, 2)))
				&& !word.contains(" ")
				&& LangProxy.sameStartAndEnd(word, typo)
				&& !typo.contains(" ");
	}
}
