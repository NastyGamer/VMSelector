package selector;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import lombok.*;
import org.firmata4j.Pin;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Switch {

    @Getter(lazy = true)
    private final VMInterface vmInterface = newInterface();

    private VMInterface newInterface() {
        switch (interfaceName) {
            case "virsh" -> {
                return new VirshInterface();
            }
            case "proxmox" -> {
                return new ProxmoxInterface();
            }
            default -> throw new IllegalStateException("Unexpected value: " + interfaceName);
        }
    }

    private final String interfaceName;
    public static final Switch SWITCH = deserialize(Path.of("/etc", "VMSwitch"));
    @Getter
    private final List<SwitchEntry> entries;

    @SneakyThrows
    public static Switch deserialize(Path configFile) {
        if (!Files.exists(configFile)) throw new NullPointerException("Config file doesn't exist");
        @Cleanup final BufferedReader reader = Files.newBufferedReader(configFile);
        final JsonValue v =Json.parse(reader);
        return new Switch(v.asObject().get("interfaceName").asString(),
                v.asObject().get("switches")
                .asArray()
                .values()
                .stream()
                .map(value -> SwitchEntry.deserialize(value.asObject()))
                .toList()
        );
    }

    public List<String> getVmNames() {
        return entries
                .stream()
                .map(SwitchEntry::getName)
                .toList();
    }

    public List<Pin> getButtonPins() {
        return entries
                .stream()
                .map(SwitchEntry::getButtonPin)
                .toList();
    }

    public List<Integer> getButtonPinNumbers() {
        return entries
                .stream()
                .map(SwitchEntry::getButton)
                .toList();
    }

    public List<Pin> getLedPins() {
        return entries
                .stream()
                .map(SwitchEntry::getLedPin)
                .toList();
    }

    public List<Integer> getLedPinNumbers() {
        return entries
                .stream()
                .map(SwitchEntry::getLed)
                .toList();
    }

    public SwitchEntry find(String name) {
        return entries.stream().filter(entry -> entry.getName().equals(name)).findFirst().get();
    }

    public int getIndexOf(SwitchEntry entry) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).equals(entry)) return i;
        }
        return -1;
    }

    public int getSize() {
        return entries.size();
    }
}
