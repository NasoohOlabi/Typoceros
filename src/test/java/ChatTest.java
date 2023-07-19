import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class ChatTest {
	static String[] chat = new String[] {
			" We are destroying our home. We have one planet at the moment. I think that forest are hard to produce. ",
			"Interesting",
			"Are you a fan of Google or Microsoft?",
			"Saltwater fish drink water through the mouth. Dolphins are friendly to human beings.",
			"It is very interesting. Google provide \"Chrome OS\"which is a light weight OS. Google provided a lot of hardware mainly in 2010 to 2015.",
			"Both are excellent technology they are helpful in many ways. For the security purpose both are super.",
			"That would be cool to visit. Did you know Iceland has no public rail system and most people travel by train there?",
			"I'm not  a huge fan of Google",
			"but I use it a lot because I have to. I think they are a monopoly in some sense.",
			"Google provides online related services and products", "which includes online ads",
			"search engine and cloud computing.", "Yeah",
			"their services are good. I'm just not a fan of intrusive they can be on our personal lives.",
			"Google is leading the alphabet subsidiary and will continue to be the Umbrella company for Alphabet internet interest.",
			"Did you know Google had hundreds of live goats to cut the grass in the past?",
			"I like Google Chrome. Do you use it as well for your browser?",
			"Yes.Google is the biggest search engine and Google service figure out top 100 website",
			"including Youtube and Blogger.", "By the way", "do you like Fish?",
			"Yes. They form a sister group of tourniquets- they make the sea water clean and remove the dust from it. Fish is the biggest part in the eco-system.",
			"Did you know that a seahorse is the only fish to have a neck?",
			"Freshwater fish only drink water through the skin via Osmosis",
			"they also have gills. Did you know that jellyfish are immortal?",
			"Yes. Fish is the important resources of human world wide for the commercial and subsistence fish hunts the fish in the wild fisheries.",
			"What about cats", "do you like cats? I'm a dog fan myself.",
			"The cat is referred as domestic cat and wild cat. They make our world very clean from rats!", "Yeah",
			"cats can be cool", "but they sure do spend a lot of their time sleeping.",
			"Cats hear the sounds too faint or too high frequency human ears can hear.", "I heard that too. Well",
			"it was nice chatting with you. Have a good day.",
			"That would be cool to visit. Did you know Iceland has no public rail system and most people travel by air there?" };

	private final Logger _logger = new Logger("ChatTest");
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
	public void spellWord() throws IOException {
		_logger.log(ThinLangApi.spellWord("priduce"));
	}

	@Test
	public void testTexts() {
		var tt = new TypoTest();
		for (var ex : chat) {
			try {
				tt.testString(ex, TypoTest.generateRandomBitStream(ex.length(), 0));
			} catch (ValueError | IOException e) {
				_logger.error("testStringExtensive", e);
			}
		}
	}

	public List<Integer> getBitsFromSpaces(List<Integer> spaces) throws IOException {
		var bits = spaces.stream().map(util::log2).collect(Collectors.toList());
		_logger.debug(bits.toString());
		return bits;
	}

	@Test
	public void testBitEncoding() throws IOException, ValueError {
		var spaces = List.of(82, 0, 11);
		for (int i = 0; i < 2000; i++) {

			var bytes_str = TypoTest.generateRandomBitStream(30);
			_logger.trace("bytes_str", bytes_str);
			var values_rem = Typo.encode_encoder(bytes_str, spaces, getBitsFromSpaces(spaces));
			var values = values_rem._1;
			var rem = values_rem._2;
			_logger.trace("values", values);
			_logger.trace("rem", rem);
			var des = Typo.decode_decoder(values, spaces, getBitsFromSpaces(spaces));
			_logger.trace("des", des);

			assertEquals("bit encoders work", bytes_str, des + rem);
		}

	}
}
