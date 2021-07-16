package selector;

import com.eclipsesource.json.JsonObject;
import com.google.common.base.Objects;
import lombok.*;
import org.firmata4j.Pin;

import java.io.IOException;

import static selector.PinMap.HIGH;
import static selector.PinMap.LOW;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class SwitchEntry {

    private final String name;
    private final int button;
    private final int led;
    private Thread blinkThread;
    @Getter
    @Setter
    private Pin buttonPin;
    @Getter
    @Setter
    private Pin ledPin;
    @Getter
    private LED_TYPE type;
    @Getter
    @Setter
    private boolean running;

    public static SwitchEntry deserialize(JsonObject object) {
        return new SwitchEntry(object.get("vmName").asString(), object.get("buttonPin").asInt(), object.get("ledPin").asInt());
    }

    public void startBlinkThread() {
        this.blinkThread = new Thread(() -> {
            while (true) {
                if (type != LED_TYPE.BLINKING) continue;
                try {
                    ledPin.setValue(ledPin.getValue() == LOW ? HIGH : LOW);
                    Thread.sleep(500L);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        this.blinkThread.setDaemon(true);
        this.blinkThread.start();
    }

    @SneakyThrows
    public void setType(LED_TYPE type) {
        this.type = type;
        switch (type) {
            case PERMANENT -> ledPin.setValue(HIGH);
            case OFF -> ledPin.setValue(LOW);
            case BLINKING -> {
            }
            default -> {
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SwitchEntry that = (SwitchEntry) o;
        return Objects.equal(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
