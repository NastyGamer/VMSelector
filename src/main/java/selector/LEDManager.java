package selector;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LEDManager {

    public void apply(LEDConfig config) {
        for (int i = 0; i < config.types().length; i++) {
            Switch.SWITCH.getEntries().get(i).setType(config.types()[i]);
        }
    }
}
