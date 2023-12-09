package lang;

import java.util.regex.Pattern;

public class Rule {
    public final Pattern regex;
    public final String repl;

    public Rule(Pattern regex, String repl) {
        this.regex = regex;
        this.repl = repl;
    }

    public static Rule of(Pattern regex,String repl){
        return  new Rule(regex,repl);
    }
}
