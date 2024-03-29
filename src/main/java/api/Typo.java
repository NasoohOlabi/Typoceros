package api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import common.Buckets;
import common.Logger;
import common.util;
import lang.LangProxy;
import lang.TypoMatch;

public class Typo {
    private final static Logger _logger = Logger.named("api.Typo");
    private final String text;
    private Buckets<TypoMatch> buckets;
    private static int span_size = 10;

    public Typo(String text) throws IllegalArgumentException, IOException {
        _logger.info(String.format("api.Typo constructor: text %s", text));
        this.text = LangProxy.normalize(text, getSpanSize());
        if (!isAcceptable(text, this.text)) {
            throw new IllegalArgumentException(String.format(
                    "text '%s' normalizes to '%s'", text, this.text));
        }
        _logger.info(String.format("this.text = '%s'", this.text));
    }

    public boolean isAcceptable(String text, String normalized) throws IOException {
        return text.equals(normalized);
    }

    public EncodeResult encode(String values) throws IllegalArgumentException, IOException, ValueError {
        var tmp = encode_encoder(values, getBuckets().getSpaces(), getBuckets().getBits());
        return new EncodeResult(_encode(tmp.bit_values()), tmp.remaining_bits());
    }

    public static DecoderResult decode(String text) throws IOException {
        return decode(text, null);
    }

    public String _encode(List<Integer> values) throws IllegalArgumentException, IOException {
        _logger.debugSeparatorStart();
        var spaces = getBuckets().getSpaces();
        if (values.size() > spaces.size()) {
            throw new IllegalArgumentException("Can't encode");
        }

        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) >= spaces.get(i) && spaces.get(i) != 0) {
                throw new IllegalArgumentException("Won't fit");
            }
        }

        String result = this.text;
        _logger.debug("api.Typo.encode: encoding " + values + " in '" + this.text + "'");
        _logger.debug("Phase 0: " + result);
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i) != 0)
                result = getBuckets().getSlot(i, values.get(i)).makeTypoInOriginal.apply(result);
            _logger.debug("Phase "
                    + (values.size() - i)
                    + ": " + result);
        }
        _logger.debugSeparatorEnd();
        return result;
    }

    public static DecoderResult decode(String text, Typo test_self)
            throws IOException {
        String original = LangProxy.normalize(text, getSpanSize());
        _logger.info("original=" + original);
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
        var values = t._decode(text, test_self);
        return new DecoderResult(original, values, Typo.decode_decoder(values, t.getBuckets().getSpaces(), t.getBuckets().getBits()));
    }

    public List<Integer> _decode(String text, Typo test) throws IOException {
        Typo a_self = test != null ? test : this;
        var spaces = a_self.getBuckets().getSpaces();
        int cnt = util.diff(text, a_self.text).size();
        _logger.debug("cnt=" + cnt);
        _logger.debug("util.diff('" + text + "','" + a_self.text + "')=" + util.diff(text, a_self.text));
        List<Integer> values = new ArrayList<>(Collections.nCopies(spaces.size(), 0));
        for (int i = 0; i < spaces.size(); i++) {
            for (int j = 0; j < spaces.get(i); j++) {
                var dif = util.diff(text, a_self._encode(values));
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

    private static Integer pow(Integer b, Integer e) {
        return (int) Math.pow((double) b, (double) e);
    }

    public static EncodeEncoderResult encode_encoder(String bytes_str, List<Integer> spaces,
            List<Integer> bits_list)
            throws ValueError, IOException {
        if (!new HashSet<>(Arrays.asList('0', '1'))
                .containsAll(new HashSet<>(bytes_str.chars().mapToObj(c -> (char) c).collect(Collectors.toList())))) {
            throw new ValueError("bytes_str isn't a bytes string : '" + bytes_str + "'");
        }
        // _logger.traceSeparatorStart();
        // _logger.trace(String.format(
        // "encode_encoder(bytes_str: %s, spaces: %s, bits_list: %s)",
        // bytes_str, spaces.toString(), bits_list.toString()));
        List<Integer> bit_values = new ArrayList<>();
        String remaining_bits = bytes_str;
        for (int i = 0; i < bits_list.size(); i++) {
            int bits = bits_list.get(i);
            if (remaining_bits.length() >= bits + 1
                    && Integer.parseInt(remaining_bits.substring(0, bits + 1), 2) < spaces.get(i)
                    && Integer.parseInt(remaining_bits.substring(0, bits + 1), 2) >= pow(2, bits)) {
                int bit_value = Integer.parseInt(remaining_bits.substring(0, bits + 1), 2);
                // _logger.trace(String.format(
                // "int %d = Integer.parseInt(remaining_bits.substring(0, %d + 1): %s, 2);",
                // bit_value, bits, remaining_bits.substring(0, bits + 1)));
                bit_values.add(bit_value);
                remaining_bits = remaining_bits.substring(bits + 1);
            } else if (remaining_bits.length() >= bits && bits > 0) {
                int bit_value = Integer.parseInt(remaining_bits.substring(0, bits), 2);
                // _logger.trace(String.format(
                // "int %d = Integer.parseInt(remaining_bits.substring(0, %d): %s, 2);",
                // bit_value, bits, remaining_bits.substring(0, bits)));
                bit_values.add(bit_value);
                remaining_bits = remaining_bits.substring(bits);
            } else {
                bit_values.add(0);
            }
        }
        return new EncodeEncoderResult(bit_values, remaining_bits);
    }

    /**
     *
     * @param values is the actual encoded values
     * @param spaces is the space of the bucket
     * @param bits_list is the bits size of the bucket
     * @return returns bits string
     */
    public static String decode_decoder(List<Integer> values, List<Integer> spaces, List<Integer> bits_list) {
        // _logger.trace(
        // String.format("decode_decoder(values: %s, bits_list: %s)", values.toString(),
        // bits_list.toString()));
        List<String> res = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            var space = spaces.get(i);
            var value_to_convert = values.get(i);
            if (space == 0) {
                continue;
            }
            var bits = bits_list.get(i);
            // _logger.trace("bits", bits);
            // _logger.trace("value_to_bits", value_to_convert);
            String v = Integer.toBinaryString(value_to_convert);
            // _logger.trace("not padded", v);
            // _logger.trace("not padded length", v.length());
            v = ("0".repeat(Math.max(bits - v.length(), 0))) + v;
            // _logger.trace("padded", v);
            res.add(v);
        }
        // _logger.trace("decode_decoder=>", res);
        return String.join("", res);
    }

    public static int getSpanSize() {
        return Typo.span_size;
    }

    public static void setSpanSize(int value) {
        Typo.span_size = value;
    }

    public Buckets<TypoMatch> getBuckets() throws IOException {
        if (buckets == null){
            _logger.info("generating slots!");
            var slots = LangProxy.valid_rules_scan(this.text, span_size);
            buckets = new Buckets<>(util.chunk(text,span_size),slots,TypoMatch::getSourceSpan);
        }
        return buckets;
    }

}