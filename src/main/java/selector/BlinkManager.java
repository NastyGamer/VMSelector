package selector;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.firmata4j.Pin;

import java.io.IOException;

import static selector.PinMap.*;

@UtilityClass
public class BlinkManager {

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
			BlinkManager.stopLeftPermanent();
			BlinkManager.stopRightPermanent();
		}));
	}

	public void startBlinkLeft() {
		isBlinkingLeft = true;
	}

	public void startBlinkRight() {
		isBlinkingRight = true;
	}

	@SneakyThrows
	public void stopBlinkLeft() {
		isBlinkingLeft = false;
		PIN_LED_LEFT.setValue(LOW);
	}

	@SneakyThrows
	public void stopBlinkRight() {
		isBlinkingRight = false;
		PIN_LED_RIGHT.setValue(LOW);
	}

	@SneakyThrows
	public void startLeftPermanent() {
		isBlinkingLeft = false;
		PinMap.PIN_LED_LEFT.setValue(HIGH);
	}

	@SneakyThrows
	public void startRightPermanent() {
		isBlinkingRight = false;
		PinMap.PIN_LED_RIGHT.setValue(HIGH);
	}

	@SneakyThrows
	public void stopLeftPermanent() {
		isBlinkingLeft = false;
		PinMap.PIN_LED_LEFT.setValue(LOW);
	}

	@SneakyThrows
	public void stopRightPermanent() {
		isBlinkingRight = false;
		PinMap.PIN_LED_RIGHT.setValue(LOW);
	}
}
