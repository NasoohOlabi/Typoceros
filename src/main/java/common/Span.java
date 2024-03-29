package common;

import java.util.Optional;
import java.util.function.Function;

import io.vavr.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.languagetool.rules.RuleMatch;

public class Span implements Comparable<Span> {
    public final int start;
    public final int end;

    public Span(int start, int end) {
        assert 0 <= start;
        assert start < end;
        this.start = start;
        this.end = end;
    }

    public static Span of(int start, int end) {
        return new Span(start, end);
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public boolean contain(Span other) {
        return this.start <= other.start && other.end <= this.end;
    }

    public boolean intersects(Span other) {
        return this.start < other.end && other.start < this.end;
    }

    public Optional<Span> intersection(Span other) {
        int intersectionStart = Math.max(this.start, other.start);
        int intersectionEnd = Math.min(this.end, other.end);

        if (intersectionStart < intersectionEnd) {
            return Optional.of(Span.of(intersectionStart, intersectionEnd));
        } else {
            return Optional.empty(); // No intersection
        }
    }

    @Override
    public String toString() {
        return "Span{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }

    public String in(String text) {
        assert start >= 0;
        assert end <= text.length();
        return text.substring(start, end);
    }

    public String notIn(String text) {
        return text.substring(0, start) + text.substring(end);
    }

    public String swap(String text, String repl) {
        return text.substring(0, start) + repl + text.substring(end);
    }

    public Tuple2<Span, String> swapAndUpdate(String text, String repl) {
        return new Tuple2<>(Span.of(start, start + repl.length()), text.substring(0, start) + repl + text.substring(end));
    }

    public Span map(Function<Integer, Integer> f) {
        return Span.of(f.apply(start), f.apply(end));
    }

    public static Span fromRule(RuleMatch ruleMatch) {
        return Span.of(ruleMatch.getFromPos(), ruleMatch.getToPos());
    }

    public Span union(Span other) {
        return Span.of(Math.min(this.getStart(), other.getStart()),
                Math.max(this.getEnd(), other.getEnd()));
    }

    @Override
    public int compareTo(@NotNull Span o) {
        return (this.start - o.start != 0)
                ? this.start - o.start
                : this.end - o.end;
    }
}
