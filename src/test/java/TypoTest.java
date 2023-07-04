import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.stream.IntStream;
import java.util.stream.Stream;

//import org.apache.log4j.core.config.Configurator;
import org.junit.Test;

import io.vavr.Tuple2;

public class TypoTest {
    private final Logger _logger = LogManager.getLogger("Typoceros.TypoTest");


    public static String generateRandomBitStream(int length, long seed) {

        StringBuilder sb = new StringBuilder();
        Random random = new Random(seed);

        for (int i = 0; i < length; i++) {
            int bit = random.nextInt(2);
            sb.append(bit);
        }

        return sb.toString();
    }

    String[] examples = {
            "hi, how are you?",
            "I sure do. You can listen to JUpiter's storms on AM radio",
//            "Hey, How are you? Did you see the last John Cena movie?", "Hi, How are you?",
//            "However, you may as well just use a function statement instead; the only advantage that a lambda offers is that you can put a function definition for a simple expression inside a larger expression.",
            "However, you may as well just use a function statement instead; the only advantage that a lambda offers is that you can put a function definition for a simple expression inside a larger expression. But the above lambda is not part of a larger expression, it is only ever part of an assignment statement, binding it to a name. That's exactly what a statement would achieve.",
            "I’ve toyed with the idea of using GPT-3’s API to add much more intelligent capabilities to RC, but I can’t deny that I’m drawn to the idea of running this kind of language model locally and in an environment I control. I’d like to someday increase the speed of RC’s speech synthesis and add a speech-to-text translation model in order to achieve real-time communication between humans and the chatbot. I anticipate that with this handful of improvements, RC will be considered a fully-fledged member of our server. Often, we feel that it already is."
    };

    public static Stream<List<Integer>> spacesTest(List<Integer> spaces) {
        return spacesTest(spaces, 0);
    }

    public static Stream<List<Integer>> spacesTest(List<Integer> spaces, int start) {
        int max = Collections.max(spaces);
        int size = spaces.size();
        return IntStream.range(start, max)
                .mapToObj(i -> {
                    var array = new ArrayList<Integer>(size);
                    for (Integer space : spaces) {
                        array.add(i % space);
                    }
                    return array;
                });
    }

    @Test
    public void santiyCheck() {
        assertTrue(List.of("TYPOS", "SPELLING", "GRAMMAR", "TYPOGRAPHY")
                .contains("TYPOS"));
    }

    @Test
    public void vote_fix_word_test_examples() throws IOException {
        vote_fix_word_test("I arte an apple", "ate");
        vote_fix_word_test("We arte feeling happy", "are");
    }

    public void vote_fix_word_test(String text, String fixedWord)
            throws IOException {
        _logger.info("\n" + "#".repeat(50) + "\nvote_fix_word_test");
        _logger.info("text=(" + text + ")");
        _logger.info("fixedWord is=(" + fixedWord + ")");
        var textSpans = new StringSpans(text);
        var a = LangProxy.vote_fix_word(
                textSpans.getWords().get(1), text);
        assertEquals(fixedWord, a);
    }

    @Test
    public void spellCheckEdges() throws IOException {
        var alone = LangProxy.check("arte");
        _logger.info(alone.toString());
        for (var rm : alone) {
            _logger.info(rm.getSuggestedReplacements().toString());
        }
        var context = LangProxy.check("I arte an apple");
        _logger.info(context.toString());
        for (var rm : context) {
            _logger.info(rm.getSuggestedReplacements().toString());
        }
        var context_wrong_meaning = LangProxy.check("I are an apple");
        _logger.info(context_wrong_meaning.toString());
        for (var rm : context_wrong_meaning) {
            _logger.info(rm.getSuggestedReplacements().toString());
        }

        assertEquals(
                "speech-to-text",
                LangProxy.normalize(
                        "sopeech-to-text", 10, true));
    }

    public void testString(String text, String bytes) throws ValueError, IOException {
        _logger.info("#".repeat(25) + "\nTest String\n" + "#".repeat(25));
        _logger.info("text='" + text + "'");
        _logger.info("bytes='" + bytes + "'");
        var typo = new Typo(text);
        Timer.startTimer("testString: '" + text + "'");
        _logger.info("typo.spaces\t" + typo.getSpaces().toString());
        Timer.prettyPrint("testString: '" + text + "'");
        var byteList_rem = typo.encode_encoder(bytes);
        _logger.info("byteList_rem\t" + byteList_rem.toString());
        var encoded = typo.encode(byteList_rem._1);
        _logger.info("encoded\t" + encoded);
        var decoded_byteList = Typo.decode(encoded, null);
        _logger.info("decoded_byteList\t" + decoded_byteList);

        assertEquals(text, text, decoded_byteList._1);
        assertEquals(byteList_rem._1.size(), decoded_byteList._2.size());
        for (int i = 0; i < byteList_rem._1.size(); i++) {
            assertEquals(byteList_rem._1.get(i), decoded_byteList._2.get(i));
        }
    }

    public void testStringExtensive(String text) throws ValueError, IOException {
        _logger.info("#".repeat(25) + "\nTest String\n" + "#".repeat(25));
        _logger.info("text='" + text + "'");
        var typo = new Typo(text);
        Timer.startTimer("testString: '" + text + "'");
        _logger.info("typo.spaces\t" + typo.getSpaces().toString());
        Timer.prettyPrint("testString: '" + text + "'");
        final String sep = "v".repeat(55);
        spacesTest(typo.getSpaces(),140).forEach(values -> {
            _logger.info(sep);
            _logger.info("values: " + values);
            Timer.startTimer(
                    "testString: '" + text + "' values: " + values.toString());
            String encoded = null;
            try {
                encoded = typo.encode(values);
            } catch (IOException e) {
                _logger.error("testStringExtensive", e);
            }
            _logger.info("encoded\t" + encoded);
            Tuple2<String, List<Integer>> decoded_byteList = null;
            try {
                decoded_byteList = Typo.decode(encoded, typo);
            } catch (IOException e) {
                _logger.error("testStringExtensive", e);
            }
            _logger.info("decoded_byteList\t" + decoded_byteList);

            assert decoded_byteList != null;
            assertEquals(text, decoded_byteList._1);
            assertEquals(values.size(), decoded_byteList._2.size());
            for (int i = 0; i < values.size(); i++) {
                assertEquals(values.get(i), decoded_byteList._2.get(i));
            }
            Timer.prettyPrint(
                    "testString: '" + text + "' values: " + values);
        });
    }

    @Test
    public void testNormalize() throws IOException {
        _logger.info("Test started");
        assertEquals("hi, how are you?", LangProxy.normalize("hi, how are yiou?", 10, true));
        assertEquals(examples[1], LangProxy.normalize(examples[1], 10,true));
        for (var ex : examples) {
            assertEquals(ex, LangProxy.normalize(ex, 10, false));
        }
    }

    // @Test
    // public void testControl() {
    // for (var ex :
    // examples) {
    // try {
    // testString(ex, generateRandomBitStream(ex.length(), 0), false);
    // } catch (ValueError | IOException e) {
    // _logger.error("testControl", e);
    // }
    // }
    // }

    @Test
    public void testControl_idx_1() {

        var ex = examples[1];
        try {
            testString(ex, generateRandomBitStream(ex.length(), 0));
        } catch (ValueError | IOException e) {
            _logger.error("testStringExtensive", e);
        }

    }

    @Test
    public void testAll() {
        for (var ex :
                examples) {
            try {
                testStringExtensive(ex);
            } catch (ValueError | IOException e) {
                _logger.error("testAll", e);
            }
        }
    }
}
