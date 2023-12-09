package lang;

import common.Span;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public class TypoMatch implements Comparable<TypoMatch> {
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

    public static TypoMatch of(String text, String after, Span sourceSpan, Function<String, String> apply) {
        return new TypoMatch(text, after, sourceSpan, apply);
    }

    @Override
    public String toString() {
        return String.format("|>change (%s)\n%s\n%s\n", sourceSpan.in(text), text, after);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TypoMatch typoMatch))
            return false;

        // Cast o to lang.TypoMatch type

        return after.equals(typoMatch.after) && sourceSpan.equals(typoMatch.sourceSpan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(after, sourceSpan);
    }

    @Override
    public int compareTo(@NotNull TypoMatch o) {
        return this.sourceSpan.compareTo(o.sourceSpan);
    }

    public String getText() {
        return text;
    }

    public String getAfter() {
        return after;
    }

    public Span getSourceSpan() {
        return sourceSpan;
    }

    public Function<String, String> getMakeTypoInOriginal() {
        return makeTypoInOriginal;
    }
}
