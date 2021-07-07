package selector;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.firmata4j.IODevice;
import org.firmata4j.firmata.FirmataDevice;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static selector.PinMap.HIGH;

public class Main {

	private static final ImmutableMap<STATE, LEDConfig> LEDMap = ImmutableMap.copyOf(Map.of(
			STATE.STARTED, new LEDConfig(LED_TYPE.PERMANENT, LED_TYPE.PERMANENT),
			STATE.BUSY_STARTING_VM_1, new LEDConfig(LED_TYPE.PERMANENT, LED_TYPE.BLINKING),
			STATE.BUSY_STARTING_VM_2, new LEDConfig(LED_TYPE.BLINKING, LED_TYPE.PERMANENT),
			STATE.VM1_STARTED, new LEDConfig(LED_TYPE.PERMANENT, LED_TYPE.OFF),
			STATE.VM2_STARTED, new LEDConfig(LED_TYPE.OFF, LED_TYPE.PERMANENT),
			STATE.CONFIRM_SWITCH_TO_VM_1, new LEDConfig(LED_TYPE.BLINKING, LED_TYPE.PERMANENT),
			STATE.CONFIRM_SWITCH_TO_VM_2, new LEDConfig(LED_TYPE.PERMANENT, LED_TYPE.BLINKING)
	));

	private static final String[] vmNames = new String[2];
	private static Timer vm1Timer;
	private static Timer vm2Timer;

	@SneakyThrows
	public static void main(String[] args) {
		if (args.length != 2) throw new IllegalArgumentException("Illegal arg length: " + args.length);
		System.arraycopy(args, 0, vmNames, 0, 2);
		VMInterface.validateVMNames(vmNames);
		System.out.println("Registered VMs: " + Arrays.toString(vmNames));
		var ref = new Object() {
			StateMachine<STATE, TRIGGER> machine = null;
		};
		final StateMachineConfig<STATE, TRIGGER> config = new StateMachineConfig<>();

		config.configure(STATE.STARTED).permit(TRIGGER.LEFT_BUTTON_PRESSED, STATE.BUSY_STARTING_VM_1);
		config.configure(STATE.STARTED).permit(TRIGGER.RIGHT_BUTTON_PRESSED, STATE.BUSY_STARTING_VM_2);
		config.configure(STATE.STARTED).ignore(TRIGGER.DONE_STARTING_VM_1);
		config.configure(STATE.STARTED).ignore(TRIGGER.DONE_STARTING_VM_2);

		config.configure(STATE.BUSY_STARTING_VM_1).permit(TRIGGER.DONE_STARTING_VM_1, STATE.VM1_STARTED);
		config.configure(STATE.BUSY_STARTING_VM_1).ignore(TRIGGER.DONE_STARTING_VM_2);
		config.configure(STATE.BUSY_STARTING_VM_1).ignore(TRIGGER.LEFT_BUTTON_PRESSED);
		config.configure(STATE.BUSY_STARTING_VM_1).ignore(TRIGGER.RIGHT_BUTTON_PRESSED);

		config.configure(STATE.BUSY_STARTING_VM_2).permit(TRIGGER.DONE_STARTING_VM_2, STATE.VM2_STARTED);
		config.configure(STATE.BUSY_STARTING_VM_2).ignore(TRIGGER.DONE_STARTING_VM_1);
		config.configure(STATE.BUSY_STARTING_VM_2).ignore(TRIGGER.LEFT_BUTTON_PRESSED);
		config.configure(STATE.BUSY_STARTING_VM_2).ignore(TRIGGER.RIGHT_BUTTON_PRESSED);

		config.configure(STATE.VM1_STARTED).permit(TRIGGER.RIGHT_BUTTON_PRESSED, STATE.CONFIRM_SWITCH_TO_VM_2);
		config.configure(STATE.VM1_STARTED).ignore(TRIGGER.LEFT_BUTTON_PRESSED);
		config.configure(STATE.VM1_STARTED).ignore(TRIGGER.DONE_STARTING_VM_1);
		config.configure(STATE.VM1_STARTED).ignore(TRIGGER.DONE_STARTING_VM_2);

		config.configure(STATE.VM2_STARTED).permit(TRIGGER.LEFT_BUTTON_PRESSED, STATE.CONFIRM_SWITCH_TO_VM_1);
		config.configure(STATE.VM2_STARTED).ignore(TRIGGER.RIGHT_BUTTON_PRESSED);
		config.configure(STATE.VM2_STARTED).ignore(TRIGGER.DONE_STARTING_VM_1);
		config.configure(STATE.VM2_STARTED).ignore(TRIGGER.DONE_STARTING_VM_2);

		config.configure(STATE.CONFIRM_SWITCH_TO_VM_1).permit(TRIGGER.LEFT_BUTTON_PRESSED, STATE.BUSY_STARTING_VM_1);
		config.configure(STATE.CONFIRM_SWITCH_TO_VM_1).permit(TRIGGER.RIGHT_BUTTON_PRESSED, STATE.VM2_STARTED);
		config.configure(STATE.CONFIRM_SWITCH_TO_VM_1).ignore(TRIGGER.DONE_STARTING_VM_1);
		config.configure(STATE.CONFIRM_SWITCH_TO_VM_1).ignore(TRIGGER.DONE_STARTING_VM_2);

		config.configure(STATE.CONFIRM_SWITCH_TO_VM_2).permit(TRIGGER.RIGHT_BUTTON_PRESSED, STATE.BUSY_STARTING_VM_2);
		config.configure(STATE.CONFIRM_SWITCH_TO_VM_2).permit(TRIGGER.LEFT_BUTTON_PRESSED, STATE.VM1_STARTED);
		config.configure(STATE.CONFIRM_SWITCH_TO_VM_2).ignore(TRIGGER.DONE_STARTING_VM_1);
		config.configure(STATE.CONFIRM_SWITCH_TO_VM_2).ignore(TRIGGER.DONE_STARTING_VM_2);

		config.configure(STATE.STARTED).onEntry(() -> {
			System.out.println("Blinking");
			LEDManager.apply(LEDMap.get(STATE.STARTED));
		});

		config.configure(STATE.BUSY_STARTING_VM_1).onEntry(() -> {
			LEDManager.apply(LEDMap.get(STATE.BUSY_STARTING_VM_1));
			VMInterface.stopVM(vmNames[1]);
			VMInterface.startVM(vmNames[0]);
			ref.machine.fire(TRIGGER.DONE_STARTING_VM_1);
		});
		config.configure(STATE.BUSY_STARTING_VM_2).onEntry(() -> {
			LEDManager.apply(LEDMap.get(STATE.BUSY_STARTING_VM_2));
			VMInterface.stopVM(vmNames[0]);
			VMInterface.startVM(vmNames[1]);
			ref.machine.fire(TRIGGER.DONE_STARTING_VM_2);
		});
		config.configure(STATE.CONFIRM_SWITCH_TO_VM_1).onEntry(() -> {
			LEDManager.apply(LEDMap.get(STATE.CONFIRM_SWITCH_TO_VM_1));
			vm1Timer = new Timer("WaitSwitchVM1", true);
			vm1Timer.schedule(new TimerTask() {
				@Override
				public void run() {
					ref.machine.fire(TRIGGER.RIGHT_BUTTON_PRESSED);
					vm1Timer = null;
				}
			}, 5000L);
		});
		config.configure(STATE.CONFIRM_SWITCH_TO_VM_2).onEntry(() -> {
			LEDManager.apply(LEDMap.get(STATE.CONFIRM_SWITCH_TO_VM_2));
			vm2Timer = new Timer("WaitSwitchVM2", true);
			vm2Timer.schedule(new TimerTask() {
				@Override
				public void run() {
					ref.machine.fire(TRIGGER.LEFT_BUTTON_PRESSED);
					vm2Timer = null;
				}
			}, 5000L);
		});
		config.configure(STATE.VM1_STARTED).onEntry(() -> {
			LEDManager.apply(LEDMap.get(STATE.VM1_STARTED));
			if (vm1Timer != null) {
				vm1Timer.cancel();
				vm1Timer.purge();
				vm1Timer = null;
			}
		});
		config.configure(STATE.VM2_STARTED).onEntry(() -> {
			LEDManager.apply(LEDMap.get(STATE.VM2_STARTED));
			if (vm2Timer != null) {
				vm2Timer.cancel();
				vm2Timer.purge();
				vm2Timer = null;
			}
		});
		final IODevice device = new FirmataDevice("/dev/ttyACM0");
		device.start();
		device.ensureInitializationIsDone();
		PinMap.initializePins(device);
		LEDManager.initialize();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				device.stop();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}));
		PinMap.PIN_BUTTON_LEFT.addEventListener((PinChangeListener) newValue -> {
			if (newValue == HIGH) return;
			ref.machine.fire(TRIGGER.LEFT_BUTTON_PRESSED);
		});

		PinMap.PIN_BUTTON_RIGHT.addEventListener((PinChangeListener) newValue -> {
			if (newValue == HIGH) return;
			ref.machine.fire(TRIGGER.RIGHT_BUTTON_PRESSED);
		});
		ref.machine = new StateMachine<>(STATE.STARTED, config);
		ref.machine.fireInitialTransition();
	}

	private enum STATE {
		STARTED,
		BUSY_STARTING_VM_1,
		BUSY_STARTING_VM_2,
		VM1_STARTED,
		VM2_STARTED,
		CONFIRM_SWITCH_TO_VM_2,
		CONFIRM_SWITCH_TO_VM_1
	}

	private enum TRIGGER {
		LEFT_BUTTON_PRESSED,
		RIGHT_BUTTON_PRESSED,
		DONE_STARTING_VM_1,
		DONE_STARTING_VM_2
	}
}
