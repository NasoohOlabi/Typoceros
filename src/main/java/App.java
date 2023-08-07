
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
        System.setOut(new PrintStream(System.out, true, "UTF-8"));
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name())) {
            while (true) {
                command = scanner.nextLine();
                string = from64(scanner.nextLine());
                Config.sync();
                if (command.equalsIgnoreCase("encode")) {
                    bytes = scanner.nextLine();
                    typo = getTypo(string);
                    var encoded_rem = typo.encode(bytes);
                    System.out.println(to64(encoded_rem._1()));
                    System.out.println(encoded_rem._2);
                } else if (command.equalsIgnoreCase("decode")) {
                    var originalText_values_bits = Typo.decode(string, null);
                    var originalText = originalText_values_bits._1();
                    System.out.println(to64(originalText));
                    System.out.println(originalText_values_bits._3());
                } else if (command.equalsIgnoreCase("echo")) {
                    System.out.println(to64(string));
                }
                // Config.flushLogs();
            }
        } catch (IllegalArgumentException | IOException | ValueError e) {
            _logger.error("App in main ", e);
        } finally {
            Logger.closeAll();
        }
    }

    public static String to64(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        byte[] encodedBytes = Base64.getEncoder().encode(bytes);
        return new String(encodedBytes, StandardCharsets.UTF_8);
    }

    public static String from64(String s) {
        byte[] decodedBytes = Base64.getDecoder().decode(s.getBytes(StandardCharsets.UTF_8));
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}
