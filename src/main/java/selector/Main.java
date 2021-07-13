package selector;

import lombok.SneakyThrows;
import org.firmata4j.IODevice;
import org.firmata4j.firmata.FirmataDevice;

import static selector.PinMap.HIGH;
import static selector.Switch.SWITCH;

public class Main {

	@SneakyThrows
	public static void main(String[] args) {
		VMInterface.checkVirsh();
		VMInterface.validateVMNames(SWITCH.getVmNames());
		final IODevice device = new FirmataDevice("/dev/VMSwitch");
		device.start();
		device.ensureInitializationIsDone();
		PinMap.initializePins(device, SWITCH.getEntries());
		LEDManager.apply(new LEDConfig(LED_TYPE.OFF, LED_TYPE.OFF, LED_TYPE.OFF, LED_TYPE.OFF));
		SWITCH.getEntries().forEach(SwitchEntry::startBlinkThread);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			@SneakyThrows
			public void run() {
				LEDManager.apply(new LEDConfig(LED_TYPE.OFF, LED_TYPE.OFF, LED_TYPE.OFF, LED_TYPE.OFF));
				device.stop();
			}
		}));
		SWITCH.getEntries().forEach(entry -> entry.getButtonPin().addEventListener((PinChangeListener) newValue -> {
			if (newValue == HIGH || entry.getName().equals("-")) return;
			final int entryId = SWITCH.getIndexOf(entry);
			for (int i = 0; i < SWITCH.getEntries().size(); i++) {
				if (i == entryId) continue;
				SWITCH.getEntries().get(i).setType(LED_TYPE.OFF);
			}
			entry.setType(LED_TYPE.PERMANENT);
			VMInterface.switchToVM(VMInterface.getRunningVM(), entry);
		}));
	}
}
