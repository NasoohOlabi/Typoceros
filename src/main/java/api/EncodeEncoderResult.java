package api;

import java.util.List;

public record EncodeEncoderResult(List<Integer> bit_values, String remaining_bits) {
}
