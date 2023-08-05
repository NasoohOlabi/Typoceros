import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import io.vavr.Tuple2;

public class BenchmarkChatTest {

	private final Logger _logger = Logger.named("ChatTest");
	private static boolean setUpIsDone = false;

	@Before
	public void setUp() {
		if (setUpIsDone) {
			return;
		}
		Config.sync();
		setUpIsDone = true;
	}

	public String stripQuotes(String s) {
		if (s.startsWith("\""))
			return s.substring(1, s.length() - 1);
		else
			return s;
	}

	public Stream<Tuple2<String, String>> chats() {
		InputStream inputStream = Rules.class.getClassLoader().getResourceAsStream("chats.csv");
		assert inputStream != null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		return reader.lines()
				.map(String::strip)
				.filter(line -> !line.isEmpty())
				.map(line -> {
					var fc = line.indexOf(",");

					return new Tuple2<>(line.substring(0, fc), stripQuotes(line.substring(fc + 1)));
				});
	}

	@Test
	public void testTexts() {
		var tt = new TypoTest();
		chats().dropWhile(x -> Integer.parseInt(x._1) < 100).forEach(chatId_ex -> {
			var chatId = chatId_ex._1;
			var ex = chatId_ex._2;
			try {
				var startTime = new java.util.Date().getTime();
				var bits = tt.testString(ex, TypoTest.generateRandomBitStream(ex.length(), 0), true);
				var endTime = new java.util.Date().getTime();
				long durationMillis = endTime - startTime;
				int durationSecs = (int) (durationMillis / 1000);
				int mins = durationSecs / 60;
				int secs = durationSecs % 60;

				try (var fw = new FileWriter(new File("benchmarks2.0.tsv"), true)) {
					fw.write(
							String.format("%s\t%d\t%d\t%d\t%d:%02d\n", chatId, ex.length(), bits, (bits * 100) / ex.length(), mins,
									secs));
				} catch (Exception e) {
					// TODO: handle exception
				}
			} catch (ValueError | IOException e) {
				_logger.error("testStringExtensive", e);
			}
		});
	}

	public List<Integer> getBitsFromSpaces(List<Integer> spaces) throws IOException {
		var bits = spaces.stream().map(util::log2).collect(Collectors.toList());
		_logger.debug(bits.toString());
		return bits;
	}

}
