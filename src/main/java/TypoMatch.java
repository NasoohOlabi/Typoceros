import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class TypoMatch {
    public final String text;
    public final String after;
    public final Span sourceSpan;
    public final Function<String, String> makeTypoInOriginal;

    public TypoMatch(String text, String after, Span sourceSpan, Function<String, String> makeTypoInOriginal) {
        this.text = text;
        this.after = after;
        this.sourceSpan = sourceSpan;
        this.makeTypoInOriginal = makeTypoInOriginal;
    }

    public static boolean isFirstCharUpperCase(String str) {
        if (str != null && !str.isEmpty()) {
            char firstChar = str.charAt(0);
            return Character.isUpperCase(firstChar);
        }
        return false;
    }

    public static Optional<TypoMatch> of(String text, String after, Span sourceSpan, Function<String, String> apply) {
        if (isFirstCharUpperCase(sourceSpan.in(text)))
            return Optional.empty();
        else
            return Optional.of(new TypoMatch(text, after, sourceSpan, apply));
    }

    @Override
    public String toString() {
        return String.format("|>change (%s)\n%s\n%s\n", sourceSpan.in(text), text, after);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TypoMatch))
            return false;

        TypoMatch typoMatch = (TypoMatch) o; // Cast o to TypoMatch type

        return after.equals(typoMatch.after) && sourceSpan.equals(typoMatch.sourceSpan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(after, sourceSpan);
    }
}
