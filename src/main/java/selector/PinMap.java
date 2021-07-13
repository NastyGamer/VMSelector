package selector;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.firmata4j.IODevice;
import org.firmata4j.Pin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

@UtilityClass
public class PinMap {

	public static final long LOW = 0;
	public static final long HIGH = 1;

	@SneakyThrows
	public void initializePins(@NotNull final IODevice device, @NotNull final List<SwitchEntry> switchEntries) {
		switchEntries.forEach(new Consumer<SwitchEntry>() {
			@SneakyThrows
			@Override
			public void accept(SwitchEntry switchEntry) {
				switchEntry.setLedPin(device.getPin(switchEntry.getLed()));
				switchEntry.setButtonPin(device.getPin(switchEntry.getButton()));
				switchEntry.getLedPin().setMode(Pin.Mode.OUTPUT);
				switchEntry.getLedPin().setValue(LOW);
				switchEntry.getButtonPin().setMode(Pin.Mode.PULLUP);
			}
		});
	}
}
