package selector;

import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ProxmoxInterface implements VMInterface {

    private SwitchEntry running;

    @Override
    public void validateVMNames(List<String> names) {
        names = names.stream().filter(s -> !s.equals("-")).toList();
        final List<String> vms = Arrays.stream(runAndGet("qm", "list").getValue0()
                .split("\n"))
                .map(String::trim)
                .map(s -> s.split(" +")[1])
                .toList();
        if (!vms.containsAll(names)) throw new IllegalArgumentException("Invalid vm name(s)");
    }

    @Override
    public void checkInstallation() {
        if (runAndGet("qm", "help").getValue2() != 0)
            throw new NullPointerException("Unable to locate qm binary");
        else System.out.println("Proxmox installation seems okay");
    }

    @SneakyThrows
    private int stopVM(SwitchEntry entry) {
        if (isShutdown(entry)) {
            System.out.println("VM " + entry.getName() + " already shutdown");
            return 0;
        }
        System.out.println("Stopping VM " + entry.getName());
        System.out.println("Attempting graceful shutdown");
        final int exitCode = runAndForward("qm", "shutdown", String.valueOf(mapSwitchToId(entry)));
        for (int i = 0; i < 120; i++) {
            System.out.println("Attempting shutdown nr. " + (i + 1) + "/120");
            if (isShutdown(entry)) {
                System.out.println("Graceful shutdown succeeded after attempt nr. " + (i + 1));
                return exitCode;
            }
            Thread.sleep(5000L);
        }
        System.out.println("Destroying vm");
        return runAndForward("qm", "stop", String.valueOf(mapSwitchToId(entry)));
    }

    private int startVM(SwitchEntry entry) {
        System.out.println("Starting VM " + entry.getName());
        return runAndForward("qm", "start", String.valueOf(mapSwitchToId(entry)));
    }

    private int mapSwitchToId(SwitchEntry entry) {
        return Arrays.stream(runAndGet("qm", "list").getValue0()
                .split("\n"))
                .map(String::trim)
                .filter(s -> s.split(" +")[1].equals(entry.getName()))
                .map(s -> Integer.parseInt(s.split(" +")[0]))
                .findFirst()
                .orElseThrow();
    }

    private boolean isShutdown(SwitchEntry entry) {
        return Arrays.stream(runAndGet("qm", "list").getValue0()
                .split("\n"))
                .map(String::trim)
                .filter(s -> s.split(" +")[1].equals(entry.getName()))
                .findFirst()
                .orElseThrow()
                .split(" +")[2].equals("stopped");
    }

    @Override
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
