import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.vavr.Tuple2;

public class Typo {
    private final static Logger _logger = new Logger("./Typoceros/logs/Typoceros.Typo");
    private final String text;
    private List<TypoMatch> _slots = null;
    private List<Integer> _spaces = null;
    private List<Span> _chunks = null;
    private static int span_size = 10;

    public int getLength() throws IOException {
        return getSlots().size();
    }

    public List<TypoMatch> getSlots() throws IOException {
        if (this._slots == null) {
        _logger.info("generating slots!");
            this._slots = LangProxy.valid_rules_scan(this.text, span_size);
        }
        return this._slots;
    }

    public TypoMatch getSlot(int idx) throws IOException {
        return getSlots().get(idx);
    }

    public TypoMatch getSlot(int space, int offset) throws IOException {
        assert offset != 0;
        return getSlots().get(getSlotIdx(space, offset));
    }

    public List<Span> getChunks() {
        if (_chunks == null) {
            var span_size = getSpanSize();
            _chunks = util.chunk(text, span_size);
        }
        return _chunks;
    }

    public List<Integer> getSpaces() throws IOException {
        if (_spaces != null) {
            return _spaces;
        }
        _logger.info("generating Spaces");
        var sentenceRanges = getChunks();

        // Initialize an empty list of buckets
        int numBuckets = sentenceRanges.size();
        List<Integer> buckets = new ArrayList<>(Arrays.asList(new Integer[numBuckets]));
        Collections.fill(buckets, 0);

        // Iterate through each element range and put it in the corresponding bucket
        for (int i = 0; i < this.getSlots().size(); i++) {
            var slot = this.getSlots().get(i).sourceSpan;
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
        _logger.debug(_spaces.toString());
        return buckets;
    }

    public List<Integer> getBits() throws IOException {
        var bits = getSpaces().stream().map(util::log2).collect(Collectors.toList());
        _logger.debug(bits.toString());
        return bits;
    }

    public Typo(String text) throws IllegalArgumentException, IOException {
        _logger.info(String.format("Typo constructor: text %s", text));
        this.text = LangProxy.normalize(text, getSpanSize());
        if (!isAcceptable(text, this.text)) {
            throw new IllegalArgumentException(String.format(
                    "text '%s' normalizes to '%s'", text, this.text
            ));
        }
        _logger.info(String.format("this.text = '%s'", this.text));
    }

    public boolean isAcceptable(String text, String normalized) throws IOException {
        return text.equals(normalized);
    }

    public boolean isAcceptable(String text) throws IOException {
        return text.equals(LangProxy.normalize(text, getSpanSize()));
    }

    public String FixText(String text) throws IOException {
        return LangProxy.normalize(text, getSpanSize());
    }

    public int getSlotIdx(int space, int offset) throws IOException {
        if (offset == 0) {
            return 0;
        }
        return util.sum(this.getSpaces().subList(0, space)) + offset - 1;
    }

    public String encode(List<Integer> values) throws IllegalArgumentException, IOException {
        _logger.debugSeparatorStart();
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
        _logger.debug("Typo.encode: encoding " + values + " in '" + this.text + "'");
        _logger.debug("Phase 0: " + result);
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i) != 0)
                result = getSlot(i, values.get(i)).makeTypoInOriginal.apply(result);
            _logger.debug("Phase "
                    + (values.size() - i)
                    + ": " + result);
        }
        _logger.debugSeparatorEnd();
        return result;
    }

    public static Tuple2<String, List<Integer>> decode(String text, Typo test_self)
            throws IOException {
        String original = LangProxy.normalize(text, getSpanSize());
        _logger.debug("original=" + original);
        Typo t;
        if (test_self != null) {
            if (!original.equals(test_self.text)) {
                _logger.debug("test_self.text=" + test_self.text);
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
        _logger.debug("cnt=" + cnt);
        _logger.debug("util.diff('" + text + "','" + a_self.text + "')=" + util.diff(text, a_self.text));
        List<Integer> values = new ArrayList<>(Collections.nCopies(spaces.size(), 0));
        for (int i = 0; i < spaces.size(); i++) {
            for (int j = 0; j < spaces.get(i); j++) {
                var dif = util.diff(text, a_self.encode(values));
                if (dif.size() == cnt - 1) {
                    _logger.debug("values=" + values);
                    _logger.debug("dif=" + dif);
                    cnt--;
                    break;
                }
                values.set(i, j + 1);
            }
            if (Objects.equals(values.get(i), spaces.get(i))) {
                values.set(i, 0);
                _logger.debug("chunk is empty values=" + values);
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
        LangProxy.normalize(text, span_size, true);
    }

    public static int getSpanSize() {
        return Typo.span_size;
    }

    public static void setSpanSize(int value) {
        Typo.span_size = value;
    }
}