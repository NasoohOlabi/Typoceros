package common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Buckets<T> {
    private final static Logger _logger = Logger.named("Buckets");
    private final Function<T,Span> toSpan;
    private List<T> _slots = null;
    private List<Integer> _spaces = null;
    private List<Span> _chunks = null;

    public int getLength() throws IOException {
        return getSlots().size();
    }

    public List<T> getSlots() throws IOException {
        return this._slots;
    }

    public T getSlot(int idx) throws IOException {
        return getSlots().get(idx);
    }

    public T getSlot(int space, int offset) throws IOException {
        assert offset != 0;
        return getSlots().get(getSlotIdx(space, offset));
    }

    public List<Span> getChunks() {
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
            var slot = toSpan.apply(this.getSlots().get(i));
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
        _logger.info("Spaces=" + _spaces.toString());

        return buckets;
    }

    public List<Integer> getBits() throws IOException {
        var bits = getSpaces().stream().map(util::log2).collect(Collectors.toList());
        _logger.info("Bits=" + bits.toString());
        return bits;
    }

    public Integer getBits(int i) throws IOException {
        return getBits().get(i);
    }

    public Buckets(List<Span> chunks, List<T> slots, Function<T,Span> toSpan) throws IllegalArgumentException, IOException {
        this._slots = slots;
        this.toSpan = toSpan;
        this._chunks = chunks;
    }


    public int getSlotIdx(int space, int offset) throws IOException {
        if (offset == 0) {
            return 0;
        }
        return util.sum(this.getSpaces().subList(0, space)) + offset - 1;
    }

}