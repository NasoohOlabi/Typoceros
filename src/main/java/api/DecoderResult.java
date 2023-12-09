package api;

import java.util.List;

public record DecoderResult(String Original, List<Integer> values, String bits) {
}
