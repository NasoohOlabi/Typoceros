import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vavr.Tuple2;
import io.vavr.Tuple3;

public class LangProxy {
    private final static ListPersistenceManager dict = new ListPersistenceManager(Config.dict_file);

    private final static Logger LangProxy_logger = new Logger("./Typoceros/logs/LangProxy");
    private final static Logger normalize_logger = new Logger("./Typoceros/logs/LangProxy.normalize");
    private static Logger _logger = LangProxy_logger;

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
                    _logger.trace(String.format("LangProxy.word_we_misspelled(word: %s, spelling: %s) = true", word,
                            spelling));
                    _logger.debug(String.format("FAT_CORRECTION_RULES (%s) (%s): %s == %s", entry._1, entry._2,
                            m.replaceAll(entry._2), spelling));
                    return true;
                }
            }
        }
        _logger.trace(String.format("LangProxy.word_we_misspelled(word: %s, spelling: %s) = false", word, spelling));
        return false;
    }

    private static List<Span> getAffectedWords(String text, StringSpans text_sss, List<Span> offsets) {
        _logger.trace(
                String.format("getAffectedWords(text: %s, text_sss: %s, offsets: %s)",
                        text,
                        text_sss.toString(),
                        offsets.toString()));
        var affected_words = new ArrayList<Span>();
        for (var o : offsets) {
            for (var word : text_sss.getWords()) {
                if (o.intersects(word)) {
                    affected_words.add(word);
                    break;
                }
            }
        }
        _logger.trace("affected_words=" + affected_words);
        return affected_words;
    }

    public static String normalize(String text, int span_size) throws IOException {
        return normalize(text, span_size, false);
    }

    public static String normalize(String text, int span_size, boolean learn) throws IOException {
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
        final StringSpans text_sss = new StringSpans(text);
        var offsets = ThinLangApi.correction_rules_subset(text)
                .stream()
                .map(Span::fromRule)
                .map(text_sss::unionIntersects)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        boolean[] empty_chunks = new boolean[chunks.size()];

        var offsets_chunks = new ArrayList<List<Tuple2<Span, String>>>();
        for (var chunk : chunks) {
            var chunk_offsets = new ArrayList<Tuple2<Span, String>>();

            for (Span affected_word : offsets) {
                if (affected_word.intersects(chunk)) {
                    var affected_word_correction_option = vote_fix_word(affected_word, text);
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

    public static Optional<String> vote_fix_word(
            Span word,
            String text) throws IOException {
        _logger.trace("---------------------vote_fix_word---------------------");
        _logger.trace(String.format("LangProxy.vote_fix_word(word='%s',text='%s'", word.in(text), text));
        List<String> suggestions = new ArrayList<>();
        _logger.trace("step 1: give LanguageTool the whole text");
        var context_suggestions = ThinLangApi.check_pos(word, text);
        context_suggestions.ifPresent(ruleMatch -> suggestions.addAll(ThinLangApi
                .filterIntentionalTypos(word.in(text), ruleMatch
                        .getSuggestedReplacements())));
        _logger.trace("suggestions", suggestions);
        _logger.trace("step 2: give LanguageTool the word alone and apply reverse rules and filter them");
        suggestions.addAll(word_corrections(
                word.in(text)));
        _logger.trace("suggestions", suggestions);
        _logger.trace("step 3: try every correction in context");
        suggestions.addAll(
                suggestions
                        .stream()
                        .flatMap((correction) -> {
                            try {
                                var result = ThinLangApi.check_pos(word,
                                        word.swap(text, correction));
                                if (result.isPresent()) {
                                    return ThinLangApi
                                            .filterIntentionalTyposStream(word.in(text),
                                                    result.get().getSuggestedReplacements());
                                }
                                return Stream.empty();
                            } catch (IOException e) {
                                _logger.error("vote_fix_word", e);
                                return Stream.empty();
                            }
                        }).collect(Collectors.toList()));
        _logger.trace("suggestions", suggestions);
        _logger.trace(suggestions.toString());
        if (suggestions.size() == 0) {
            _logger.trace("vote_fix_word Out of Suggestions");
            _logger.trace("Word", word);
            _logger.trace("Text", text);
            return Optional.empty();
        }
        var most_voted_word = util.mode(suggestions);
        _logger.trace("most voted word", most_voted_word);
        return Optional.of(most_voted_word);
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
        _logger.trace(String.format("word_corrections(typo: %s)", typo));
        var suggestions = ThinLangApi.spellWord(typo);
        _logger.trace("LanguageTool correction", suggestions);
        List<String> votes = new ArrayList<>(suggestions);
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

        _logger.trace("unfiltered votes: ", votes);
        // if correction rules were false negative
        // so they tried to correct but they were wrong
        votes.removeIf(vote -> {
            try {
                return !ThinLangApi.normal_word(vote);
            } catch (IOException e) {
                _logger.error("vote_fix_word", e);
                return true;
            }
        });
        _logger.trace("filtered votes: ", votes);
        _logger.trace("------------ word corrections end ------------");
        return votes;
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
        // _logger.trace(String.format("applyMatch(text, regex: %s, repl %s, span: %s)",
        // regex, repl, span));
        try {
            var replaced_text = regex.matcher(span.in(text)).replaceAll(repl);

            // _logger.trace(String.format("'%s' -> '%s'", span.in(text), replaced_text));

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
        List<String> mutations_fail_reason = new ArrayList<>(Collections.nCopies(slots.size(), ""));
        for (int match_index = 0; match_index < slots.size(); match_index++) {
            _logger.trace(String.format("checking slot %d/%d", match_index, slots.size()));
            var match_result = slots.get(match_index);
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

            var new_word_corrections_mode_opt = vote_fix_word(
                    newWordSpan,
                    newText);

            if (new_word_corrections_mode_opt.isEmpty()) {
                continue;
            }
            String new_word_corrections_mode = new_word_corrections_mode_opt.get();
            if (sameStartAndEnd(old_word, new_word)
                    && new_word_corrections_mode.equals(old_word)
                    && !old_word.equals(new_word)) {
                mutations.set(match_index, newText);
            } else {
                mutations_fail_reason.set(match_index,
                        "rule undetectable or modify looks! new word \"" + new_word + "\" != \""
                                + old_word + "\" original and will be corrected to " + new_word_corrections_mode);
            }
        }
        List<Integer> ambiguous_invalid_matches = new ArrayList<>();
        for (int i = 0; i < mutations.size(); i++) {
            String new_string = mutations.get(i);

            if (new_string.isEmpty()) {
                _logger.debug("mutation is ambiguous because" + mutations_fail_reason.get(i));
                ambiguous_invalid_matches.add(i);
            } else if (mutations.subList(0, i).contains(new_string)) {
                _logger.debug("mutation '" + new_string + "' is ambiguous because " +
                        "previous slot yields the same typo");
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
        _logger.debug("\n" + "%".

                repeat(20) + "valid slots!" + "%".

                        repeat(20));
        _logger.debug("valid_slots=" + valid_slots);
        _logger.debug("\n" + "%".

                repeat(20) + "valid slots!" + "%".

                        repeat(20));
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

    static List<TypoMatch> valid_rules_scan(String text, int span_size) throws IOException {
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
