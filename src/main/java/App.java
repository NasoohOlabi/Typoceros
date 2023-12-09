
import api.Typo;
import api.ValueError;
import common.Config;
import common.Logger;

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
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name())) {
            while (true) {
                command = scanner.nextLine();
                string = from64(scanner.nextLine());
                Config.sync();
                if (command.equalsIgnoreCase("encode")) {
                    bytes = scanner.nextLine();
                    typo = getTypo(string);
                    var encoded_rem = typo.encode(bytes);
                    System.out.println(to64(encoded_rem.encoded()));
                    System.out.println(encoded_rem.remaining_bits());
                } else if (command.equalsIgnoreCase("decode")) {
                    var result = Typo.decode(string, null);
                    var originalText = result.Original();
                    System.out.println(to64(originalText));
                    System.out.println(result.bits());
                } else if (command.equalsIgnoreCase("echo")) {
                    System.out.println(to64(string));
                }
                // common.Config.flushLogs();
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
