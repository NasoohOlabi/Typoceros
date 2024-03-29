package lang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import common.*;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import lang.Rules;

public class LangProxy {

	private final static Logger LangProxy_logger = Logger.named("LangProxy");
	private final static Logger normalize_logger = Logger.named("LangProxy.normalize");
	private static Logger _logger = LangProxy_logger;

	public static List<Span> detectErrorsSpans(String text) throws IOException {
		final StringSpans text_sss = new StringSpans(text);

		return ThinLangApi.correction_rules_subset(text)
				.stream()
				.map(Span::fromRule)
				.flatMap(text_sss::IntersectsList)
				.filter((potentialTypo) -> {
					try {
						_logger.trace("potentialTypo", potentialTypo);
						var spellings = ThinLangApi.spellWord(potentialTypo.in(text));
						_logger.trace("spellings", spellings);
						return spellings.size() != 0;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return true;
				})
				.collect(Collectors.toList());
	}

	public static String normalize(String text, int span_size) throws IOException {
		_logger = normalize_logger;
		if (text.length() == 0) {
			_logger.error("text.length() == 0,text=" + text);
			throw new IllegalArgumentException("Empty text");
		}
		_logger.trace("____________________________");
		_logger.trace(String.format("normalize(text: %s, span_size: %d)", text, span_size));
		List<Span> chunks = util.chunk(text, span_size);
		_logger.trace("chunks", chunks.toString());
		String to_be_original = text;
		var offsets = detectErrorsSpans(text);
		boolean[] empty_chunks = new boolean[chunks.size()];

		var offsets_chunks = new ArrayList<List<Tuple2<Span, String>>>();
		for (var chunk : chunks) {
			var chunk_offsets = new ArrayList<Tuple2<Span, String>>();

			for (Span affected_word : offsets) {
				if (affected_word.intersects(chunk)) {
					var affected_word_correction_option = word_correction(affected_word, text);
					if (affected_word_correction_option.isPresent()) {
						var affected_word_correction = affected_word_correction_option.get();
						chunk_offsets.add(new Tuple2<>(affected_word, affected_word_correction));
						_logger.trace("Added (" + affected_word + ", " + affected_word + ", " + affected_word_correction
								+ ") to chunk_offsets");
					} else {
						_logger.trace("Discarded (" + affected_word + ", " + affected_word + ") unfixable");
					}
				}
			}
			offsets_chunks.add(chunk_offsets);
			_logger.trace("Added " + chunk_offsets + " to offsets_chunks");
		}
		_logger.trace("chunks_offsets", offsets_chunks);
		for (int i = offsets_chunks.size() - 1; i >= 0; i--) {
			var offsets_chunk = offsets_chunks.get(i);
			if (offsets_chunk.size() == 1) {
				var correction = offsets_chunk.get(0)._2;
				_logger.trace("typo=" +
						offsets_chunk.get(0)._1 +
						"\tsuggestion=" +
						correction);
				to_be_original = offsets_chunk.get(0)._1.swap(to_be_original, correction);
			} else {
				empty_chunks[i] = true;
			}
		}

		_logger.trace_info(
				String.format("normalize(text: '%s', span_size: %d):\t'%s'", text, span_size, to_be_original));
		_logger.trace("__________ normalize end __________");
		_logger = LangProxy_logger;
		return to_be_original;
	}

	public static String insertCharAtIndex(String str, char ch, int index) {
		if (index < 0 || index > str.length()) {
			throw new IndexOutOfBoundsException("Index is out of bounds");
		}
		StringBuilder sb = new StringBuilder(str);
		sb.insert(index, ch);
		return sb.toString();
	}

	public static Optional<String> word_correction(Span word,
												   String text) throws IOException {
		Timer.startTimer("LangProxy.word_correction");
		_logger.trace(String.format("word_correction(word: %s, text: %s)", word, text));
		var typo = word.in(text);
		_logger.trace("typo", typo);
		List<String> ruleBasedCorrection = new ArrayList<>();
		for (var rule : Rules.FAT_CORRECTION_RULES) {
			Matcher matcher = rule.regex.matcher(typo);
			while (matcher.find()) {
				var span = Span.of(matcher.start(), matcher.end());
				ruleBasedCorrection.add(applyMatch(typo, rule, span));
			}
		}
		for (var rule : Rules.WORD_CORRECTION_RULES) {
			Matcher matcher = rule.regex.matcher(typo);
			if (matcher.find()) {
				ruleBasedCorrection.add(matcher.replaceAll(rule.repl));
			}
		}
		for (var rule : Rules.KEYBOARD_CORRECTION_RULES) {
			Matcher matcher = rule.regex.matcher(typo);
			while (matcher.find()) {
				var span = Span.of(matcher.start(), matcher.end());
				ruleBasedCorrection.add(applyMatch(typo, rule, span));
			}
		}
		var alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
		for (var c : alphabet) {
			for (int i = 1; i < typo.length(); i++) {
				ruleBasedCorrection.add(insertCharAtIndex(typo, c, i));
			}
		}

		_logger.trace("unfiltered votes: ", ruleBasedCorrection);
		var election = new Election(ruleBasedCorrection.size(), typo);
		// Collections.shuffle(ruleBasedCorrection);// DON'T DO ANYTHING RANDOM IF YOU
		// AREN'T USING
		// THE WHOLE LIST IDIOT!
		/**
		 * so lets explain what's going on I have a costly operation which is checking
		 * every
		 * ruleBasedCorrection if it's a valid word and if it's not taking the suggested
		 * fix to
		 * be a vote! ok
		 * if a word is long enough ruleBasedCorrection would be more 40 element
		 * which is definitely a foot gun in terms of performance
		 * I'll limit the votes - not checks!! - to 13 ~ common.Config.max_population
		 * but I can't take the same order so I have to sample
		 * doing it randomly is nothing but a headache... or I'm not sure.
		 * I could use an interval based mask 🤔
		 * OK I'm sorry since I
		 */
		Collections.shuffle(ruleBasedCorrection);
		for (int i = 0; i < ruleBasedCorrection.size(); i++) {
			var replacement_updatedString = word.swapAndUpdate(text, ruleBasedCorrection.get(i));
			var replacement = replacement_updatedString._1;
			var updatedString = replacement_updatedString._2;
			var unimportant_suggestions = ThinLangApi.suggestionsForWord(replacement, updatedString);
			if (unimportant_suggestions.isEmpty())
				continue;

			List<String> candidates = unimportant_suggestions.get()
					.stream()
					.filter(ruleBasedCorrection::contains)
					.collect(Collectors.toList());

			election.addBallet(candidates);
			// just stop
			if (election.getCurrentVotes() > Config.max_population)
				break;
			else
				_logger.trace("election.getCurrentVotes()", election.getCurrentVotes());
		}
		_logger.trace(election.report());
		var winner = election.getWinner();
		_logger.trace("fit context election winner: ", winner);
		_logger.trace("------------ word corrections end ------------");
		Timer.prettyPrint("LangProxy.word_correction", _logger);
		return winner;
	}

	/**
	 * @param text  text to replace content in
	 * @param repl  replace value to use with regex
	 * @param regex regex to match the part that should be replaced
	 * @param span  the exact position of where the match should be
	 * @return text with regex and repl applied to the right position aka span
	 */
	public static String applyMatch(
			String text, String repl, Pattern regex, Span span) {
		_logger.trace(String.format("applyMatch(text, regex: %s, repl %s, span: %s)",
				regex, repl, span));
		try {
			var replaced_text = regex.matcher(span.in(text)).replaceAll(repl);

			_logger.trace(String.format("'%s' -> '%s'", span.in(text), replaced_text));

			return span.swap(text, replaced_text);
		} catch (Exception exp) {
			_logger.debug("Exception in lang.LangProxy.java:389 applyMatch");
			_logger.debug("text:(" + text + ")");
			_logger.debug("repl:(" + repl + ")");
			_logger.debug("regex:(" + regex + ")");
			_logger.debug("span:(" + span + ")");
			_logger.error("applyMatch", exp);
			throw exp;
		}
	}

	public static String applyMatch(
			String text, Rule rule, Span span) {
		return applyMatch(text, rule.repl, rule.regex, span);
	}


	public static List<TypoMatch> valid_matches(String text,
												List<TypoMatch> slots, int span_size) throws IOException {
		StringSpans texas = new StringSpans(text);
		List<String> mutations = new ArrayList<>(Collections.nCopies(slots.size(), ""));
		List<String> mutations_fail_reason = new ArrayList<>(Collections.nCopies(slots.size(), ""));

		List<Integer> ambiguous_invalid_matches = new ArrayList<>();

		slots.parallelStream().forEach(match_result -> {
			var match_index = slots.indexOf(match_result);
			_logger.trace_progress(String.format("checking slot %d/%d", match_index, slots.size()));
			var span = match_result.sourceSpan;
			var newText = match_result.after;
			var newTexas = new StringSpans(newText);

			var oldWordSpan = texas.expand_span_to_word(span)._1();
			var newWordSpan = newTexas.expand_span_to_word(span)._1();

			String old_word = oldWordSpan.in(text);

			if (newWordSpan.end >= newText.length()) {
				_logger.error(
						"newWordSpan.end >= newText.length() => " + newWordSpan.end + ">=" + newText.length()
								+ "\tnewText="
								+ newText);
			}

			String new_word = newWordSpan.in(newText);

			if (sameStartAndEnd(old_word, new_word)
					&& !old_word.equals(new_word)) {
				mutations.set(match_index, newText);
			} else {
				mutations_fail_reason.set(match_index,
						"rule undetectable or modify looks! new word \"" + new_word + "\" != \""
								+ old_word + "\" original");
			}

			int i = match_index;
			String new_string = mutations.get(i);

			if (new_string.isEmpty()) {
				_logger.debug("mutation is ambiguous because" + mutations_fail_reason.get(i));
				ambiguous_invalid_matches.add(i);
			} else if (mutations.subList(0, i).contains(new_string)) {
				_logger.debug("mutation '" + new_string + "' is ambiguous because " +
						"previous slot yields the same typo");
				ambiguous_invalid_matches.add(i);
			} else {
				String normalized = "";
				try {
					normalized = normalize(new_string, span_size);
				} catch (IOException e) {
					_logger.error("unlikely. just retry shit hit the fan once", e);
				}
				if (!normalized.equals(text)) {
					_logger.debug("mutation '" + new_string + "' is ambiguous because " +
							"It normalizes to '" + normalized + "' while it should normalize to '" + text + "'");
					ambiguous_invalid_matches.add(i);
				}

			}
		});

		_logger.debug("ambiguous_invalid_matches=" + ambiguous_invalid_matches);
		List<TypoMatch> valid_slots = new ArrayList<>();
		for (int i = 0; i < slots.size(); i++) {
			if (!ambiguous_invalid_matches.contains(i)) {
				valid_slots.add(slots.get(i));
			}
		}
		_logger.debug("\n"
				+ "%".repeat(20)
				+ "valid slots!"
				+ "%".repeat(20));
		_logger.debug("valid_slots=" + valid_slots);
		_logger.debug("\n"
				+ "%".repeat(20)
				+ "valid slots!"
				+ "%".repeat(20));
		for (int i = 0; i < slots.size(); i++) {
			_logger.debug(slots.get(i) + " " + mutations.get(i));
		}
		return valid_slots;
	}

	public static boolean sameStartAndEnd(String old_word, String new_word) {
		return Character.toLowerCase(new_word.charAt(0)) == Character.toLowerCase(old_word.charAt(0)) &&
				(Character.toLowerCase(new_word.charAt(new_word.length() - 1)) == Character
						.toLowerCase(old_word.charAt(old_word.length() - 1)));
	}

	public static List<TypoMatch> valid_rules_scan(String text, int span_size) throws IOException {
		_logger.info("scanning for rules");
		List<TypoMatch> proposed_slots = Rules.rules_scan(text);
		_logger.info("done scanning for rules");
		_logger.debugSeparatorStart();
		_logger.debug("proposed_slots: " + proposed_slots);
		_logger.debugSeparatorEnd();
		_logger.info("validating rules");
		List<TypoMatch> valid_slots = valid_matches(text, proposed_slots, span_size);
		_logger.info("done validating rules");
		_logger.debugSeparatorStart();
		_logger.debug("valid_slots: ");
		for (var s : valid_slots) {
			_logger.debug(s.toString());
		}
		_logger.debugSeparatorEnd();
		return valid_slots;
	}

}
