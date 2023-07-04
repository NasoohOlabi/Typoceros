import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

import io.vavr.Tuple2;
import io.vavr.Tuple3;

public class LangProxy {
    private final static ListPersistenceManager dict = new ListPersistenceManager(Config.dict_file);

    private final static Logger _logger = LogManager.getLogger("Typoceros.LangProxy");

    private final static int INTENTIONAL_TYPO_STR_DIST = 1;

    private static final JLanguageTool langTool = new JLanguageTool(new AmericanEnglish());

    public static boolean isNormalWord(String word) throws IOException {
        List<RuleMatch> matches = langTool.check(word);
        return matches.isEmpty();
    }

    public static String spell_word(String word) throws IOException {
        if (normal_word(word)) {
            return word;
        }
        String spellingOpt = LangProxy.langTool.check(word).get(0).getSuggestedReplacements().get(0);
        String spelling = (spellingOpt != null) ? spellingOpt : word;
        return word_we_misspelled(word, spelling) ? spelling : word;
    }

    public static List<RuleMatch> check(String text) throws IOException {
        return langTool.check(text);
    }

    public static boolean word_we_misspelled(String word, String spelling) {
        int uls = util.countUppercaseLetters(word);
        if (Character.toLowerCase(spelling.charAt(0)) == Character.toLowerCase(word.charAt(0))
                && Character.toLowerCase(spelling.charAt(spelling.length() - 1)) == Character
                        .toLowerCase(word.charAt(word.length() - 1))
                && uls == 2
                && uls < word.length()) {

            for (var entry : Rules.FAT_CORRECTION_RULES) {
                Matcher m = entry._1.matcher(word);
                if (!m.replaceAll(entry._2).equals(spelling)) {
                    _logger.debug(String.format("FAT_CORRECTION_RULES (%s) (%s): %s == %s\n", entry._1, entry._2,
                            m.replaceAll(entry._2), spelling));
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public static List<RuleMatch> correction_rules_subset(String text) throws IOException {
        List<RuleMatch> matches = LangProxy.langTool.check(text);
        _logger.debug("matches=" + matches);
        var subset = new ArrayList<RuleMatch>();
        for (RuleMatch match : matches) {
            if (List.of("TYPOS", "SPELLING", "GRAMMAR", "TYPOGRAPHY")
                    .contains(match.getRule().getCategory().getId().toString())) {
                subset.add(match);
            } else {
                _logger.debug("Discarded match=" + match);
                _logger.debug("Discarded match id=" + match.getRule().getCategory().getId());
            }
        }
        return subset;
    }

    private static List<Span> getAffectedWords(String text, StringSpans text_sss, List<Integer> offsets) {
        var affected_words = new ArrayList<Span>();
        for (int o : offsets) {
            Span closest_word = null;
            int closest_distance = Integer.MAX_VALUE;

            for (var word : text_sss.getWords()) {
                int distance = word.dist(o);
                if (distance > 0) { // o is to the left of the current word
                    if (distance < closest_distance) {
                        closest_distance = distance;
                        closest_word = word;
                    }
                } else { // o is inside the current word
                    closest_word = word;
                    break;
                }
            }
            affected_words.add(closest_word);
        }
        return affected_words;
    }

    public static String normalize(String text, int span_size) throws IOException {
        return normalize(text, span_size, false);
    }

    public static String normalize(String text, int span_size, boolean learn) throws IOException {
        if (text.length() == 0) {
            System.out.println(text);
            throw new IllegalArgumentException("Empty text");
        }
        List<Span> chunks = util.chunk(text, span_size);
        String to_be_original = text;
        var offsets = correction_rules_subset(text)
                .stream()
                .map(RuleMatch::getFromPos)
                .collect(Collectors.toList());
        boolean[] empty_chunks = new boolean[chunks.size()];
        StringSpans text_sss = new StringSpans(text);
        List<Span> affected_words = getAffectedWords(text, text_sss, offsets);
        _logger.debug("normalize \ntext=" + text +
                "\nchunks=" + chunks.stream().map(s -> s.in(text)).collect(Collectors.toList()) +
                "\noffsets=" + offsets +
                "\ntext_sss.words=" + text_sss.getWordsStrings() +
                "\naffected_words=" + affected_words);
        var offsets_chunks = new ArrayList<List<Tuple3<Integer, Span, List<String>>>>();
        for (var chunk : chunks) {
            int chunk_start = chunk.start;
            int chunk_end = chunk.end;
            var chunk_offsets = new ArrayList<Tuple3<Integer, Span, List<String>>>();
            for (int i = 0; i < offsets.size(); i++) {
                int o = offsets.get(i);
                if (chunk_start <= o && o < chunk_end) {
                    Span affected_word = affected_words.get(i);
                    String affected_word_string = affected_word.in(text);
                    List<String> affected_word_corrections = word_corrections(affected_word_string);
                    chunk_offsets.add(new Tuple3<>(o, affected_word, affected_word_corrections));
                    _logger.trace("Added (" + o + ", " + affected_word + ", " + affected_word_corrections
                            + ") to chunk_offsets");
                }
            }
            offsets_chunks.add(chunk_offsets);
            _logger.trace("Added " + chunk_offsets + " to offsets_chunks");
        }
        _logger.debug("chunks_offsets=" + offsets_chunks);
        for (int i = offsets_chunks.size() - 1; i >= 0; i--) {
            var offsets_chunk = offsets_chunks.get(i);
            if (offsets_chunk.size() > 1) {
                _logger.debug("len(" + offsets_chunk + ")=" + offsets_chunk.size() + " > 1");
                empty_chunks[i] = true;
                if (learn) {
                    for (var x : offsets_chunk) {
                        LangProxy.addWord(x._2.in(text));
                    }
                }
            } else if (offsets_chunk.size() == 1
                    && offsets_chunk.get(0)._3.size() == 0) {
                _logger.debug(
                        "no suggestions for " +
                                offsets_chunk.get(0)._2.in(text) +
                                " added to dict");
                empty_chunks[i] = true;
                if (learn)
                    LangProxy.addWord(offsets_chunk.get(0)._2.in(text));
            } else if (offsets_chunk.size() == 1) {
                var cs = offsets_chunk.get(0)._3;
                _logger.debug("typo=" +
                        offsets_chunk.get(0)._2 +
                        "\tsuggestion=" +
                        util.mode(cs) + "\tvotes=" + cs);
                to_be_original = offsets_chunk.get(0)._2.swap(to_be_original, util.mode(cs));
            } else {
                empty_chunks[i] = true;
            }
        }

        return to_be_original;
    }

    private static Optional<RuleMatch> check_pos(Span word, String text) throws IOException {
        var check = LangProxy.check(text);
        _logger.debug("check result(" + word + ", " + text + "): " + check);
        for (var rule : check) {
            var ruleSpan = Span.of(rule.getFromPos(), rule.getToPos());
            if (word.contain(ruleSpan) || word.intersects(ruleSpan)) {
                return Optional.of(rule);
            } else {
                _logger.debug(
                        "rule not related to word " + word + " Rule " + ruleSpan);
                _logger.debug(rule.toString());
            }
        }
        return Optional.empty();
    }

    private static List<String> getSuggestions(
            String typo,
            List<String> libSuggestions) {
        return libSuggestions
                .stream()
                .filter(x -> intentionalTypo(x, typo))
                .collect(Collectors.toList());
    }

    private static Stream<String> getSuggestionsStream(
            String typo,
            List<String> libSuggestions) {
        return libSuggestions
                .stream()
                .filter(x -> intentionalTypo(x, typo));
    }

    public static String vote_fix_word(
            Span word,
            String text) throws IOException {
        _logger.debug(String.format("LangProxy.vote_fix_word(word='%s',text='%s'", word, text));
        List<String> suggestions = new ArrayList<>();
        var context_suggestions = check_pos(word, text);
        context_suggestions.ifPresent(ruleMatch -> suggestions.addAll(getSuggestions(word.in(text), ruleMatch
                .getSuggestedReplacements())));
        _logger.debug("vote_fix_word suggestions context: " + suggestions);
        suggestions.addAll(word_corrections(
                word.in(text)));
        _logger.debug("vote_fix_word suggestions context + non: " + suggestions);
        suggestions.addAll(
                suggestions
                        .stream()
                        .flatMap((correction) -> {
                            try {
                                var result = LangProxy.check_pos(word,
                                        word.swap(text, correction));
                                if (result.isPresent()) {
                                    return getSuggestionsStream(word.in(text),
                                            result.get().getSuggestedReplacements());
                                }
                                return Stream.empty();
                            } catch (IOException e) {
                                _logger.error("vote_fix_word", e);
                                return Stream.empty();
                            }
                        }).collect(Collectors.toList()));
        _logger.debug("\nvote_fix_word suggestions (context + non) tried out: " + suggestions);
        _logger.debug(suggestions.toString());
        if (suggestions.size() == 0) {
            _logger.debug("vote_fix_word Out of Suggestions");
            _logger.debug("Suggestions: " + suggestions);
            _logger.debug("Word: " + word);
            _logger.debug("Text: " + text);
        }
        return util.mode(suggestions);
    }

    public static String insertCharAtIndex(String str, char ch, int index) {
        if (index < 0 || index > str.length()) {
            throw new IndexOutOfBoundsException("Index is out of bounds");
        }
        StringBuilder sb = new StringBuilder(str);
        sb.insert(index, ch);
        return sb.toString();
    }

    public static List<String> word_corrections(String typo) throws IOException {
        var suggestion = spellWord(typo);
        List<String> votes = new ArrayList<>(suggestion);
        for (var rule : Rules.FAT_CORRECTION_RULES) {
            Matcher matcher = rule._1().matcher(typo);
            while (matcher.find()) {
                var span = Span.of(matcher.start(), matcher.end());
                votes.add(applyMatch(typo, rule, span));
            }
        }
        for (var rule : Rules.WORD_CORRECTION_RULES) {
            Matcher matcher = rule._1().matcher(typo);
            if (matcher.find()) {
                votes.add(matcher.replaceAll(rule._2));
            }
        }
        for (var rule : Rules.KEYBOARD_CORRECTION_RULES) {
            Matcher matcher = rule._1().matcher(typo);
            while (matcher.find()) {
                var span = Span.of(matcher.start(), matcher.end());
                votes.add(applyMatch(typo, rule, span));
            }
        }
        var alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        for (var c : alphabet) {
            for (int i = 1; i < typo.length(); i++) {
                votes.add(insertCharAtIndex(typo, c, i));
            }
        }

        _logger.debug("unfiltered votes: " + votes);
        // if correction rules were false negative
        // so they tried to correct but they were wrong
        votes.removeIf(vote -> {
            try {
                return !normal_word(vote);
            } catch (IOException e) {
                _logger.error("vote_fix_word", e);
                return true;
            }
        });
        _logger.debug("filtered votes: " + votes);
        return votes;
    }

    public static boolean intentionalTypo(String word, String typo) {
        return util.stringMutationDistance(word, typo) <= INTENTIONAL_TYPO_STR_DIST;
    }

    public static List<String> spellWord(String word) throws IOException {
        return spellWord(word, 1);
    }

    public static List<String> spellWord(String word, int maxExpectDistance) throws IOException {
        List<RuleMatch> result = LangProxy.langTool.check(word);
        if (result.size() > 0) {
            return getSuggestions(word,
                    result.get(0).getSuggestedReplacements());
        }
        return List.of();
    }

    public static boolean normal_word(String word) throws IOException {
        List<RuleMatch> result = LangProxy.langTool.check(word);
        return result.size() == 0;
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
        try {
            var replaced_text = regex.matcher(span.in(text)).replaceAll(repl);

            _logger.debug(String.format("'%s' -> '%s'", span.in(text), replaced_text) + " text='" + text + "'\trepl='"
                    + repl + "'\tregex='" + regex + "'\tspan='" + span + "'");

            return span.swap(text, replaced_text);
        } catch (Exception exp) {
            _logger.debug("Exception in LangProxy.java:389 applyMatch");
            _logger.debug("text:(" + text + ")");
            _logger.debug("repl:(" + repl + ")");
            _logger.debug("regex:(" + regex + ")");
            _logger.debug("span:(" + span + ")");
            _logger.error("vote_fix_word", exp);
            throw exp;
        }
    }

    public static String applyMatch(
            String text, Tuple2<Pattern, String> rule, Span span) {
        var repl = rule._2;
        var regex = rule._1;
        return applyMatch(text, repl, regex, span);
    }

    public static String applyMatch(
            String text, Tuple3<Span, String, Pattern> rule) {
        var repl = rule._2;
        var regex = rule._3;
        var span = rule._1;
        return applyMatch(text, repl, regex, span);
    }

    public static void addWord(String s) {
        dict.addItemToList(s);
    }

    public static List<TypoMatch> valid_matches(String text,
            List<TypoMatch> slots, int span_size) throws IOException {
        StringSpans texas = new StringSpans(text);
        List<String> mutations = new ArrayList<>(Collections.nCopies(slots.size(), ""));
        for (int match_index = 0; match_index < slots.size(); match_index++) {
            var match_result = slots.get(match_index);
            var span = match_result.sourceSpan;
            var newText = match_result.after;
            var newTexas = new StringSpans(newText);

            var oldWordSpan = texas.expand_span_to_word(span)._1();
            var newWordSpan = newTexas.expand_span_to_word(span)._1();

            String old_word = oldWordSpan.in(text);

            if (newWordSpan.end >= newText.length()) {
                System.out.println(newText);
            }

            String new_word = newWordSpan.in(newText);

            String new_word_corrections_mode = vote_fix_word(
                    newWordSpan,
                    newText);
            if (normal_word(old_word) &&
                    !normal_word(new_word) &&
                    Character.toLowerCase(new_word.charAt(0)) == Character.toLowerCase(old_word.charAt(0)) &&
                    (Character.toLowerCase(new_word.charAt(new_word.length() - 1)) == Character
                            .toLowerCase(old_word.charAt(old_word.length() - 1)))
                    &&
                    new_word_corrections_mode.equals(old_word)) {
                mutations.set(match_index, newText);
            } else {
                _logger.debug("rule undetectable or modify looks! new word \"" + new_word + "\" != \""
                        + old_word + "\" original and will be corrected to " + new_word_corrections_mode);
            }
        }
        List<Integer> ambiguous_invalid_matches = new ArrayList<>();
        for (int i = 0; i < mutations.size(); i++) {
            String new_string = mutations.get(i);

            if (new_string.isEmpty()) {
                _logger.debug("mutation '" + new_string + "' is ambiguous because" +
                        "it's empty\n");
                ambiguous_invalid_matches.add(i);
            } else if (mutations.subList(0, i).contains(new_string)) {
                _logger.debug("mutation '" + new_string + "' is ambiguous because " +
                        "previous slot yields the same typo\n");
                ambiguous_invalid_matches.add(i);
            } else if (!normalize(new_string, span_size).equals(text)) {
                _logger.debug("mutation '" + new_string + "' is ambiguous because " +
                        "Not correctable normalize('" + new_string
                        + "').equals('" + text + "')=" + normalize(new_string, span_size).equals(text));
                ambiguous_invalid_matches.add(i);
            }
        }
        _logger.debug("ambiguous_invalid_matches=" + ambiguous_invalid_matches);
        List<TypoMatch> valid_slots = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            if (!ambiguous_invalid_matches.contains(i)) {
                valid_slots.add(slots.get(i));
            }
        }
        _logger.debug("\n" + "%".repeat(20) + "valid slots!" + "%".repeat(20));
        _logger.debug("valid_slots=" + valid_slots);
        _logger.debug("\n" + "%".repeat(20) + "valid slots!" + "%".repeat(20));
        for (int i = 0; i < slots.size(); i++) {
            _logger.debug(slots.get(i) + " " + mutations.get(i));
        }
        return valid_slots;
    }

    static List<TypoMatch> valid_rules_scan(String text, int span_size) throws IOException {
        List<TypoMatch> proposed_slots = Rules.rules_scan(text);
        _logger.debug("proposed_slots: " + proposed_slots);
        List<TypoMatch> valid_slots = valid_matches(text, proposed_slots, span_size);
        _logger.debug("valid_slots: ");
        for (var s : valid_slots) {
            _logger.debug(s.toString());
        }
        return valid_slots;
    }

}
