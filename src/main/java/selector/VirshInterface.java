package selector;

import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class VirshInterface implements VMInterface {

    private SwitchEntry running;

    @SneakyThrows
    private int stopVM(SwitchEntry entry) {
        if (isShutdown(entry)) {
            System.out.println("VM " + entry.getName() + " already shutdown");
            return 0;
        }
        System.out.println("Stopping VM " + entry.getName());
        System.out.println("Attempting graceful shutdown");
        final int exitCode = runAndForward("sudo", "virsh", "shutdown", entry.getName(), "--mode", "acpi");
        for (int i = 0; i < 10; i++) {
            System.out.println("Attempting shutdown nr. " + (i + 1));
            if (isShutdown(entry)) {
                System.out.println("Graceful shutdown succeeded after attempt nr. " + (i + 1));
                return exitCode;
            }
            Thread.sleep(5000L);
        }
        System.out.println("Destroying vm");
        return runAndForward("sudo", "virsh", "destroy", entry.getName());
    }

    private boolean isShutdown(SwitchEntry entry) {
        final List<String> vms = Arrays.stream(runAndGet("sudo", "virsh", "-q", "list").getValue0()
                .split("\n"))
                .map(String::trim)
                .map(s -> s.split(" +")[1])
                .toList();
        return !vms.contains(entry.getName());
    }

    private int startVM(SwitchEntry entry) {
        System.out.println("Starting VM " + entry.getName());
        return runAndForward("sudo", "virsh", "start", entry.getName());
    }

    public void validateVMNames(List<String> names) {
        names = names.stream().filter(s -> !s.equals("-")).toList();
        final List<String> vms = Arrays.stream(runAndGet("sudo", "virsh", "-q", "list", "--all").getValue0()
                .split("\n"))
                .map(String::trim)
                .map(s -> s.split(" +")[1])
                .toList();
        if (!vms.containsAll(names)) throw new IllegalArgumentException("Invalid vm name(s)");
    }

    public void checkInstallation() {
        if (runAndForward("virsh", "--version") != 0)
            throw new NullPointerException("Unable to locate virsh binary");
    }

    public void switchToVM(SwitchEntry to) {
        if (Objects.equals(to, running)) {
            System.out.println("To and from are equal. Doing nothing...");
            return;
        }
        if (running != null) {
            System.out.println("Attempting to stop vm " + running.getName());
            if (stopVM(running) == 0) {
                running.setRunning(false);
            }
        }
        System.out.println("Attempting to start vm " + to.getName());
        if (startVM(to) == 0) {
            to.setRunning(true);
            running = to;
        }
    }
}
