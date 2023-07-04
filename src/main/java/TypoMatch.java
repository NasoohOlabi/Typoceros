import java.util.Objects;
import java.util.function.Function;

public class TypoMatch {
    public final String after;
    public final Span sourceSpan;
    public final Function<String,String> makeTypoInOriginal;

    public TypoMatch(String after, Span sourceSpan, Function<String, String> makeTypoInOriginal) {
        this.after = after;
        this.sourceSpan = sourceSpan;
        this.makeTypoInOriginal = makeTypoInOriginal;
    }

    public static TypoMatch of(String after, Span sourceSpan, Function<String, String> apply){
        return new TypoMatch(after,sourceSpan,apply);
    }

    @Override
    public String toString() {
        return "TypoMatch{" +
                ", sourceSpan=" + sourceSpan +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypoMatch typoMatch)) return false;
        return after.equals(typoMatch.after) && sourceSpan.equals(typoMatch.sourceSpan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(after, sourceSpan);
    }
}
