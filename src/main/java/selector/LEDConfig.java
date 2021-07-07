package selector;

import org.jetbrains.annotations.NotNull;

public record LEDConfig(@NotNull LED_TYPE left, @NotNull LED_TYPE right) {
}
