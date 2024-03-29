import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

//
import api.DecoderResult;
import api.Typo;
import api.ValueError;
import common.Config;
import common.Logger;
import common.StringSpans;
import common.Timer;
import lang.LangProxy;
import lang.ThinLangApi;
import org.junit.Before;
import org.junit.Test;

import io.vavr.Tuple3;

public class TypoTest {
    private final Logger _logger = Logger.named("TypoTest");
    private static boolean setUpIsDone = false;

    @Before
    public void setUp() {
        if (setUpIsDone) {
            return;
        }
        Config.sync();
        setUpIsDone = true;
    }

    public static String generateRandomBitStream(int length) {
        var d = new Date();
        return generateRandomBitStream(length, d.getTime());
    }

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
            " Yep, she actually makes $1123,000 per episode! Crazy huh? ",
            "I sure do. You can listen to Jupiter's storms on AM radio",
            "I agree. Only the lichens and cyanobateria would be able to thrive on Mars anyway.",
            // "Hey, How are you? Did you see the last John Cena movie?", "Hi, How are
            // you?",
            // "However, you may as well just use a function statement instead; the only
            // advantage that a lambda offers is that you can put a function definition for
            // a simple expression inside a larger expression.",
            " Did you know Tom cruise threatened Tom Kruse, the inventor of the Hoveround over the usage of his name?",
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
    public void word_correction_test_examples() throws IOException {
        word_correction_test("I arte an apple", "ate");
        // word_correction_test("We arte feeling happy", "are");
    }

    public void word_correction_test(String text, String fixedWord)
            throws IOException {
        _logger.info("\n" + "#".repeat(50) + "\nword_correction_test");
        _logger.info("text=(" + text + ")");
        _logger.info("fixedWord is=(" + fixedWord + ")");
        var textSpans = new StringSpans(text);
        var a = LangProxy.word_correction(
                textSpans.getWords().get(1), text);
        assertEquals(fixedWord, a.get());
    }

    @Test
    public void spellCheckEdges() throws IOException {
        var alone = ThinLangApi.check("arte");
        _logger.info(alone.toString());
        for (var rm : alone) {
            _logger.info(rm.getSuggestedReplacements().toString());
        }
        var context = ThinLangApi.check("I arte an apple");
        _logger.info(context.toString());
        for (var rm : context) {
            _logger.info(rm.getSuggestedReplacements().toString());
        }
        var context_wrong_meaning = ThinLangApi.check("I are an apple");
        _logger.info(context_wrong_meaning.toString());
        for (var rm : context_wrong_meaning) {
            _logger.info(rm.getSuggestedReplacements().toString());
        }

        assertEquals(
                "speech-to-text",
                LangProxy.normalize(
                        "sopeech-to-text", 10));
    }

    public Integer testString(String text, String bytes) throws ValueError, IOException {
        return testString(text, bytes, false);
    }

    public Integer testString(String text, String bytes, boolean fast) throws ValueError, IOException {
        Timer.startTimer("TypoTest.testString");
        _logger.info("#".repeat(25) + "\nTest String\n" + "#".repeat(25));
        _logger.info("text='" + text + "'");
        _logger.info("bytes='" + bytes + "'");
        var typo = new Typo(text);
        _logger.info("typo.spaces\t" + typo.getBuckets().getSpaces().toString());
        var byteList_rem = Typo.encode_encoder(bytes, typo.getBuckets().getSpaces(), typo.getBuckets().getBits());
        var byteList = byteList_rem.bit_values();
        var rem = byteList_rem.remaining_bits();
        _logger.info("byteList_rem\t" + byteList_rem.toString());
        var encoded = typo._encode(byteList);
        _logger.info("encoded\t" + encoded);
        var decoded_bits = Typo.decode(encoded, typo);
        _logger.info("decoded_byteList\t" + decoded_bits);
        Timer.prettyPrint("TypoTest.testString", _logger);

        assertEquals(text, text, decoded_bits.Original());
        assertEquals(bytes, decoded_bits.bits() + rem);
        return decoded_bits.bits().length();
    }

    public void testStringExtensive(String text) throws ValueError, IOException {
        _logger.info("#".repeat(25) + "\nTest String\n" + "#".repeat(25));
        _logger.info("text='" + text + "'");
        var typo = new Typo(text);
        _logger.info("typo.spaces\t" + typo.getBuckets().getSpaces().toString());
        final String sep = "v".repeat(55);
        spacesTest(typo.getBuckets().getSpaces(), 140).forEach(values -> {
            _logger.info(sep);
            _logger.info("values: " + values);
            String encoded = null;
            try {
                encoded = typo._encode(values);
            } catch (IOException e) {
                _logger.error("testStringExtensive", e);
            }
            _logger.info("encoded\t" + encoded);
            DecoderResult decoded_byteList = null;
            try {
                decoded_byteList = Typo.decode(encoded, typo);
            } catch (IOException e) {
                _logger.error("testStringExtensive", e);
            }
            _logger.info("decoded_byteList\t" + decoded_byteList);

            assert decoded_byteList != null;
            assertEquals(text, decoded_byteList.Original());
            assertEquals(values.size(), decoded_byteList.values().size());
            for (int i = 0; i < values.size(); i++) {
                assertEquals(values.get(i), decoded_byteList.values().get(i));
            }
        });
    }

    @Test
    public void testNormalize() throws IOException {
        _logger.info("Test started");
        // assertEquals(examples[1], lang.LangProxy.normalize(examples[1], 10, true));
        for (var ex : examples) {
            assertEquals(ex, LangProxy.normalize(ex, 10));
        }
    }

    // @Test
    // public void testControl() {
    // for (var ex :
    // examples) {
    // try {
    // testString(ex, generateRandomBitStream(ex.length(), 0), false);
    // } catch (api.ValueError | IOException e) {
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
        for (var ex : examples) {
            try {
                testStringExtensive(ex);
            } catch (ValueError | IOException e) {
                _logger.error("testAll", e);
            }
        }
    }
}
