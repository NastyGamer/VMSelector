package selector;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.firmata4j.IODevice;
import org.firmata4j.Pin;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class PinMap {

	public static final int PIN_LED_LEFT_NR = 4;
	public static final int PIN_LED_RIGHT_NR = 5;
	public static final int PIN_BUTTON_LEFT_NR = 3;
	public static final int PIN_BUTTON_RIGHT_NR = 2;
	static final Long HIGH = 1L;
	static final Long LOW = 0L;

	public static Pin PIN_LED_LEFT;
	public static Pin PIN_LED_RIGHT;
	public static Pin PIN_BUTTON_LEFT;
	public static Pin PIN_BUTTON_RIGHT;

	@SneakyThrows
	public void initializePins(@NotNull IODevice device) {
		PIN_LED_LEFT = device.getPin(PIN_LED_LEFT_NR);
		PIN_LED_RIGHT = device.getPin(PIN_LED_RIGHT_NR);
		PIN_BUTTON_LEFT = device.getPin(PIN_BUTTON_LEFT_NR);
		PIN_BUTTON_RIGHT = device.getPin(PIN_BUTTON_RIGHT_NR);
		PIN_LED_LEFT.setMode(Pin.Mode.OUTPUT);
		PIN_LED_RIGHT.setMode(Pin.Mode.OUTPUT);
		PIN_BUTTON_LEFT.setMode(Pin.Mode.PULLUP);
		PIN_BUTTON_RIGHT.setMode(Pin.Mode.PULLUP);
	}
}
