package lang;

import common.Span;

import java.util.regex.Pattern;

public class LocalRule extends Rule {
    public final Span span;

    public LocalRule(Pattern regex, String repl, Span span) {
        super(regex, repl);
        this.span = span;
    }

    public static LocalRule of(Span span, Pattern regex, String repl) {
        return new LocalRule(regex, repl, span);
    }

    public static LocalRule of(Span span, String repl, Pattern regex) {
        return new LocalRule(regex, repl, span);
    }

    public static LocalRule of(Pattern regex, String repl, Span span) {
        return new LocalRule(regex, repl, span);
    }

    public static LocalRule of(Pattern regex, Span span, String repl) {
        return new LocalRule(regex, repl, span);
    }

    public static LocalRule of(String repl, Pattern regex, Span span) {
        return new LocalRule(regex, repl, span);
    }

    public static LocalRule of(String repl,Span span, Pattern regex) {
        return new LocalRule(regex, repl, span);
    }

}
