package selector;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.javatuples.Triplet;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@UtilityClass
public class VMInterface {

	@SneakyThrows
	private int stopVM(SwitchEntry entry) {
		if (isShutdown(entry, true)) {
			System.out.println("VM " + entry.getName() + " already shutdown");
			return 0;
		}
		System.out.println("Stopping VM " + entry.getName());
		System.out.println("Attempting graceful shutdown");
		final int exitCode = runAndForward("sudo", "virsh", "shutdown", entry.getName(), "--mode", "acpi");
		//final Process shutdownProcess = Runtime.getRuntime().exec("sudo virsh shutdown " + entry.getName() + " --mode acpi");
		//shutdownProcess.waitFor();
		for (int i = 0; i < 10; i++) {
			System.out.println("Attempting shutdown nr. " + (i + 1));
			if (isShutdown(entry, true)) {
				System.out.println("Graceful shutdown succeeded after attempt nr. " + (i + 1));
				return exitCode;
			}
			Thread.sleep(5000L);
		}
		System.out.println("Destroying vm");
		//final Process destroyProcess = Runtime.getRuntime().exec("sudo virsh destroy " + entry.getName());
		//destroyProcess.waitFor();
		return runAndForward("sudo", "virsh", "destroy", entry.getName());
	}

	@SneakyThrows
	private boolean isShutdown(SwitchEntry entry, boolean checkVirsh) {
		if (checkVirsh) {
			//final Process process = Runtime.getRuntime().exec("sudo virsh -q list --all");
			//process.waitFor();
			//final List<String> vms =
			//		Arrays.stream(IOUtils.toString(process.getInputStream(), Charset.defaultCharset()).split("\n"))
			//				.map(String::trim)
			//				.filter(s -> s.contains("shut off"))
			//				.map(s -> s.split("\\ +")[1])
			//				.toList();
			final List<String> vms = Arrays.stream(runAndGet("sudo", "virsh", "-q", "list").getValue0()
					.split("\n"))
					.map(String::trim)
					.map(s -> s.split(" +")[1])
					.toList();
			return !vms.contains(entry.getName());
			//return vms.stream().anyMatch(s -> s.equals(entry.getName()));
		} else
			return !entry.isRunning();
	}

	@SneakyThrows
	private int startVM(SwitchEntry entry) {
		System.out.println("Starting VM " + entry.getName());
		//final Process process = Runtime.getRuntime().exec("sudo virsh start " + entry.getName());
		//process.waitFor();
		//return process.exitValue();
		return runAndForward("sudo", "virsh", "start", entry.getName());
	}

	@SneakyThrows
	public void validateVMNames(List<String> names) {
		//final Process process = Runtime.getRuntime().exec("sudo virsh -q list --all");
		//process.waitFor();
		names = names.stream().filter(s -> !s.equals("-")).toList();
		//final List<String> vms =
		//		Arrays.stream(IOUtils.toString(process.getInputStream(), Charset.defaultCharset()).split("\n"))
		//				.map(String::trim)
		//				.map(s -> s.split("\\ +")[1])
		//				.toList();
		final List<String> vms = Arrays.stream(runAndGet("sudo", "virsh", "-q", "list", "--all").getValue0()
				.split("\n"))
				.map(String::trim)
				.map(s -> s.split("\\ +")[1])
				.toList();
		if (!vms.containsAll(names)) throw new IllegalArgumentException("Invalid vm name(s)");
	}

	@SneakyThrows
	public static void checkVirsh() {
		//final Process process = Runtime.getRuntime().exec("virsh --version");
		//process.waitFor();
		if (runAndForward("virsh", "--version") != 0)
			throw new NullPointerException("Unable to locate virsh binary");
	}

	public void switchToVM(@Nullable SwitchEntry from, SwitchEntry to) {
		if (Objects.equals(to, from)) {
			System.out.println("To and from are equal. Doing nothing...");
			return;
		}
		if (from != null) {
			System.out.println("Attempting to stop vm " + from.getName());
			if (stopVM(from) == 0) {
				from.setRunning(false);
			}
		}
		System.out.println("Attempting to start vm " + to.getName());
		if (startVM(to) == 0) {
			to.setRunning(true);
		}
	}

	public @Nullable SwitchEntry getRunningVM() {
		return Switch.SWITCH.getEntries().stream().filter(SwitchEntry::isRunning).findFirst().orElse(null);
	}

	@SneakyThrows
	private Triplet<String, String, Integer> runAndGet(String... params) {
		final Process process = new ProcessBuilder(params).start();
		process.waitFor();
		return new Triplet<>(IOUtils.toString(process.getInputStream(), Charset.defaultCharset()), IOUtils.toString(process.getErrorStream(), Charset.defaultCharset()), process.exitValue());
	}

	@SneakyThrows
	private int runAndForward(String... params) {
		final Process process = new ProcessBuilder(params).inheritIO().start();
		process.waitFor();
		return process.exitValue();
	}
}
