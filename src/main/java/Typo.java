import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.vavr.Tuple2;
import io.vavr.Tuple3;

public class Typo {
    private final static Logger _logger = Logger.getLogger("Typoceros.log");
    private String text;
    private List<Tuple3<Span, String, Pattern>> _slots = null;
    private List<Integer> _spaces = null;

    public int getLength() throws IOException {
        return getSlots().size();
    }

    public List<Tuple3<Span, String, Pattern>> getSlots() throws IOException {
        if (this._slots == null)
            this._slots = LangProxy.valid_rules_scan(this.text);
        return this._slots;
    }

    public List<Integer> getSpaces() throws IOException {
        if (_spaces != null) {
            return _spaces;
        }
        var span_size = getSpanSize();

        var sentenceRanges = util.chunk(text, span_size);

        // Initialize an empty list of buckets
        int numBuckets = sentenceRanges.size();
        List<Integer> buckets = new ArrayList<>(Arrays.asList(new Integer[numBuckets]));
        Collections.fill(buckets, 0);

        // Iterate through each element range and put it in the corresponding bucket
        for (int i = 0; i < this.getSlots().size(); i++) {
            var slot = this.getSlots().get(i)._1;
            int start = slot.start;
            int end = slot.end;
            for (int j = 0; j < sentenceRanges.size(); j++) {
                var sentenceRange = sentenceRanges.get(j);
                int sentStart = sentenceRange.start;
                int sentEnd = sentenceRange.end;
                if (sentStart <= start && start < sentEnd && sentStart < end && end <= sentEnd) {
                    buckets.set(j, buckets.get(j) + 1);
                    break;
                }
            }
        }
        _spaces = buckets;
        return buckets;
    }

    public List<Integer> getBits() throws IOException {
        return getSpaces().stream().map(util::log2).collect(Collectors.toList());
    }

    public Typo(String text) throws IOException {
        _constructor(text);
    }

    public void _constructor(String text) throws IOException {
        this.text = text;
        if (!isAcceptable(this.text)) {
            throw new IllegalArgumentException("Text isn't spelled correctly");
        }
    }

    public boolean isAcceptable(String text) throws IOException {
        return text.equals(LangProxy.normalize(text));
    }

    public String FixText(String text) throws IOException {
        return LangProxy.normalize(text);
    }

    public String apply(int space, int offset, String text) throws IOException {
        _logger.finest("apply: space=" + space + ", offset=" + offset + ", text=" + text);
        if (offset == 0) {
            return text;
        }
        var match_tuple = this.getSlots()
                .get(util.sum(this.getSpaces().subList(0, space)) + offset - 1);
        var applied = LangProxy.applyMatch(text, match_tuple);
        _logger.finest("applied: " + applied);
        return applied;
    }

    public String encode(List<Integer> values) throws IllegalArgumentException, IOException {
        var spaces = this.getSpaces();
        if (values.size() > spaces.size()) {
            throw new IllegalArgumentException("Can't encode");
        }

        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) >= spaces.get(i) && spaces.get(i) != 0) {
                throw new IllegalArgumentException("Won't fit");
            }
        }

        String result = this.text;
        for (int i = values.size() - 1; i >= 0; i--) {
            result = this.apply(i, values.get(i), result);
        }

        return result;
    }

    public static Tuple2<String, List<Integer>> decode(String text, Typo test_self)
            throws IOException {
        String original = LangProxy.normalize(text);
        _logger.finest("original=" + original);
        Typo t;
        if (test_self != null) {
            if (!original.equals(test_self.text)) {
                _logger.finest("test_self.text=\n" + test_self.text);
            }
            assert original.equals(test_self.text);
            t = test_self;
        } else {
            t = new Typo(original);
        }
        return new Tuple2<>(original, t._decode(text, test_self));
    }

    public List<Integer> _decode(String text, Typo test) throws IOException {
        Typo a_self = test != null ? test : this;
        var spaces = a_self.getSpaces();
        int cnt = util.diff(text, a_self.text).size();
        _logger.finest("cnt=" + cnt);
        _logger.finest("util.diff('" + text + "','" + a_self.text + "')=" + util.diff(text, a_self.text));
        List<Integer> values = new ArrayList<>(Collections.nCopies(spaces.size(), 0));
        for (int i = 0; i < spaces.size(); i++) {
            for (int j = 0; j < spaces.get(i); j++) {
                var dif = util.diff(text, a_self.encode(values));
                if (dif.size() == cnt - 1) {
                    _logger.finest("values=" + values);
                    _logger.finest("dif=" + dif);
                    cnt--;
                    break;
                }
                values.set(i, j + 1);
            }
            if (Objects.equals(values.get(i), spaces.get(i))) {
                values.set(i, 0);
                _logger.finest("chunk is empty values=" + values);
            }
        }
        return values;
    }

    public Tuple2<List<Integer>, String> encode_encoder(String bytes_str) throws ValueError, IOException {
        if (!new HashSet<>(Arrays.asList('0', '1'))
                .containsAll(new HashSet<>(bytes_str.chars().mapToObj(c -> (char) c).collect(Collectors.toList())))) {
            throw new ValueError("bytes_str isn't a bytes string : '" + bytes_str + "'");
        }
        List<Integer> values = this.getBits();
        List<Integer> bit_values = new ArrayList<>();
        String remaining_bits = bytes_str;
        for (int i = 0; i < values.size(); i++) {
            int val = values.get(i);
            if (remaining_bits.length() >= val + 1 &&
                    Integer.parseInt(remaining_bits.substring(0, val + 1), 2) < this.getSpaces().get(i)) {
                int bit_value = Integer.parseInt(remaining_bits.substring(0, val + 1), 2);
                bit_values.add(bit_value);
                remaining_bits = remaining_bits.substring(val + 1);
            } else if (remaining_bits.length() >= val && val > 0) {
                int bit_value = Integer.parseInt(remaining_bits.substring(0, val), 2);
                bit_values.add(bit_value);
                remaining_bits = remaining_bits.substring(val);
            } else {
                bit_values.add(0);
            }
        }
        return new Tuple2<>(bit_values, remaining_bits);
    }

    public void learn(String text) throws IOException {
        LangProxy.normalize(text, true);
    }

    public int getSpanSize() {
        Config.sync();
        return Config.span_size;
    }
}