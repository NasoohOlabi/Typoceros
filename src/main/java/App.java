import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class App {
    private static final Logger _logger = Logger.named("App");

    private static Typo getTypo(String string) throws IOException, IllegalArgumentException {
        Typo.setSpanSize(Config.span_size);
        return new Typo(string);
    }

    public static void main(String[] args) throws IOException {
        String command;
        String string;
        String bytes;
        Typo typo;
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                command = scanner.nextLine();
                string = scanner.nextLine();
                Config.sync();
                if (command.equalsIgnoreCase("spell")) {
                    System.out.println(LangProxy.normalize(string, Config.span_size));
                } else if (command.equalsIgnoreCase("learn")) {
                    LangProxy.normalize(string, Config.span_size, true);
                } else if (command.equalsIgnoreCase("encode")) {
                    bytes = scanner.nextLine();
                    typo = getTypo(string);
                    var encoded_rem = typo.encode(bytes);
                    System.out.println(encoded_rem._1());
                    System.out.println(encoded_rem._2);
                } else if (command.equalsIgnoreCase("decode")) {
                    var originalText_values_bits = Typo.decode(string, null);
                    var originalText = originalText_values_bits._1();
                    System.out.println(originalText);
                    System.out.println(originalText_values_bits._3());
                }
                // Config.flushLogs();
            }
        } catch (IllegalArgumentException | IOException | ValueError e) {
            _logger.error("App in main ", e);
        } finally {
            Logger.closeAll();
        }
    }
}
