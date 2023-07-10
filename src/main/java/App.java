import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class App {
    private static Map<String, Typo> memo = new HashMap<>();
    private static Logger _logger = new Logger("./Typoceros/logs/Typoceros.App");

    private static Typo getTypo(String string) throws IOException, IllegalArgumentException {
        if (memo.containsKey(string)) {
            return memo.get(string);
        }
        Typo.setSpanSize(Config.span_size);
        Typo typo = new Typo(string);
        memo.put(string, typo);
        return typo;
    }

    public static void main(String[] args) {
        // Create a File object representing the directory you want to create
        new File("./Typoceros").mkdirs();
        new File("./Typoceros/config").mkdirs();
        new File("./Typoceros/logs").mkdirs();

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
                    var values_remainingBytes = typo.encode_encoder(bytes);
                    var values = values_remainingBytes._1();
                    var remainingBytes = values_remainingBytes._2;
                    System.out.println(typo.encode(values));
                    System.out.println(remainingBytes);
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
            // Config.closeLogs();
        }
    }
}
