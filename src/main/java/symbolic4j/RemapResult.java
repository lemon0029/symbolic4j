package symbolic4j;

import java.util.List;

public record RemapResult(List<Frame> frames) {

    public boolean hasFrames() {
        return this.frames != null && !this.frames.isEmpty();
    }

    public record Frame(String className, String method, String file, long line, String parameters) {
    }
}
