package selector;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.firmata4j.Pin;

import java.io.IOException;

import static selector.PinMap.*;

@UtilityClass
public class LEDManager {

	private static volatile boolean isBlinkingLeft = false;
	private static volatile boolean isBlinkingRight = false;

	public void initialize() {
		new Thread(() -> {
			final Pin pin = PIN_LED_LEFT;
			while (true) {
				if (!isBlinkingLeft) continue;
				try {
					pin.setValue(pin.getValue() == LOW ? HIGH : LOW);
					Thread.sleep(500L);
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		new Thread(() -> {
			final Pin pin = PIN_LED_RIGHT;
			while (true) {
				if (!isBlinkingRight) continue;
				try {
					pin.setValue(pin.getValue() == LOW ? HIGH : LOW);
					Thread.sleep(500L);
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LEDManager.stopLeftPermanent();
			LEDManager.stopRightPermanent();
		}));
	}

	public void apply(LEDConfig config) {
		switch (config.left()) {
			case PERMANENT -> {
				stopLeftBlinking();
				startLeftPermanent();
			}
			case BLINKING -> {
				stopLeftPermanent();
				startLeftBlinking();
			}
			case OFF -> {
				stopLeftBlinking();
				stopLeftPermanent();
			}
		}
		switch (config.right()) {
			case PERMANENT -> {
				stopRightBlinking();
				startRightPermanent();
			}
			case BLINKING -> {
				stopRightPermanent();
				startRightBlinking();
			}
			case OFF -> {
				stopRightBlinking();
				stopRightPermanent();
			}
		}
	}

	private void startLeftBlinking() {
		isBlinkingLeft = true;
	}

	private void startRightBlinking() {
		isBlinkingRight = true;
	}

	@SneakyThrows
	private void stopLeftBlinking() {
		isBlinkingLeft = false;
		PIN_LED_LEFT.setValue(LOW);
	}

	@SneakyThrows
	private void stopRightBlinking() {
		isBlinkingRight = false;
		PIN_LED_RIGHT.setValue(LOW);
	}

	@SneakyThrows
	private void startLeftPermanent() {
		isBlinkingLeft = false;
		PinMap.PIN_LED_LEFT.setValue(HIGH);
	}

	@SneakyThrows
	private void startRightPermanent() {
		isBlinkingRight = false;
		PinMap.PIN_LED_RIGHT.setValue(HIGH);
	}

	@SneakyThrows
	private void stopLeftPermanent() {
		PinMap.PIN_LED_LEFT.setValue(LOW);
	}

	@SneakyThrows
	private void stopRightPermanent() {
		PinMap.PIN_LED_RIGHT.setValue(LOW);
	}
}
