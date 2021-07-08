package selector;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

@UtilityClass
public class VMInterface {

	@SneakyThrows
	public int stopVM(String name) {
		if (isShutdown(name)) {
			System.out.println("VM " + name + " already shutdown");
			return 0;
		}
		System.out.println("Stopping VM " + name);
		System.out.println("Attempting graceful shutdown");
		final Process shutdownProcess = Runtime.getRuntime().exec("sudo virsh shutdown " + name + " --mode acpi");
		shutdownProcess.waitFor();
		for (int i = 0; i < 10; i++) {
			System.out.println("Attempting shutdown nr. " + (i + 1));
			if (isShutdown(name)) {
				System.out.println("Graceful shutdown succeeded after attempt nr. " + (i + 1));
				return shutdownProcess.exitValue();
			}
			Thread.sleep(5000L);
		}
		System.out.println("Destroying vm");
		final Process destroyProcess = Runtime.getRuntime().exec("sudo virsh destroy " + name);
		destroyProcess.waitFor();
		return destroyProcess.exitValue();
	}

	@SneakyThrows
	private boolean isShutdown(String name) {
		final Process process = Runtime.getRuntime().exec("sudo virsh -q list --all");
		process.waitFor();
		final List<String> vms =
				Arrays.stream(IOUtils.toString(process.getInputStream(), Charset.defaultCharset()).split("\n"))
						.map(String::trim)
						.filter(s -> s.contains("shut off"))
						.map(s -> s.split("\\ +")[1])
						.toList();
		return vms.stream().anyMatch(s -> s.equals(name));
	}

	@SneakyThrows
	public int startVM(String name) {
		System.out.println("Starting VM " + name);
		final Process process = Runtime.getRuntime().exec("sudo virsh start " + name);
		process.waitFor();
		return process.exitValue();
	}

	@SneakyThrows
	public void validateVMNames(String[] names) {
		final Process process = Runtime.getRuntime().exec("sudo virsh -q list --all");
		process.waitFor();
		final List<String> vms =
				Arrays.stream(IOUtils.toString(process.getInputStream(), Charset.defaultCharset()).split("\n"))
						.map(String::trim)
						.map(s -> s.split("\\ +")[1])
						.toList();
		if (!Arrays.stream(names).allMatch(vms::contains)) throw new IllegalArgumentException("Invalid vm name(s)");
	}

	@SneakyThrows
	public static void checkVirsh() {
		final Process process = Runtime.getRuntime().exec("virsh --version");
		process.waitFor();
		if (process.exitValue() != 0) throw new NullPointerException("Unable to locate virsh binary");
		System.out.println("Picked up virsh v. " + IOUtils.toString(process.getInputStream(), Charset.defaultCharset()));
	}
}
