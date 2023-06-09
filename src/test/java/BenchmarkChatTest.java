import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import io.vavr.Tuple2;

public class BenchmarkChatTest {

	private final Logger _logger = new Logger("ChatTest");

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
		chats().forEach(chatId_ex -> {
			var chatId = chatId_ex._1;
			var ex = chatId_ex._2;
			try {
				var bits = tt.testString(ex, TypoTest.generateRandomBitStream(ex.length(), 0),true);
				try (var fw = new FileWriter(new File("benchmarks.tsv"), true)) {
					fw.write(String.format("%s\t%d\t%d\t%d\n", chatId, ex.length(), bits, (bits * 100) / ex.length()));
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
