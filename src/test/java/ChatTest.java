import java.io.IOException;

import org.junit.Test;

public class ChatTest {
	static String[] chat = new String[] {
			"It is very interesting. Google provide \"Chrome OS\"which is a light weight OS. Google provided a lot of hardware mainly in 2010 to 2015.",
			"Are you a fan of Google or Microsoft?",
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
			"Saltwater fish drink water through the mouth. Dolphins are friendly to human beings.", "Interesting",
			"they also have gills. Did you know that jellyfish are immortal?",
			"Yes. Fish is the important resources of human world wide for the commercial and subsistence fish hunts the fish in the wild fisheries.",
			"What about cats", "do you like cats? I'm a dog fan myself.",
			"The cat is referred as domestic cat and wild cat. They make our world very clean from rats!", "Yeah",
			"cats can be cool", "but they sure do spend a lot of their time sleeping.",
			"Cats hear the sounds too faint or too high frequency human ears can hear.", "I heard that too. Well",
			"it was nice chatting with you. Have a good day.",
			"That would be cool to visit. Did you know Iceland has no public rail system and most people travel by air there?" };

	private final Logger _logger = new Logger("./Typoceros/logs/Typoceros.ChatTest");

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
}
