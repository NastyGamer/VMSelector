package selector;

import org.firmata4j.IOEvent;
import org.firmata4j.PinEventListener;

@FunctionalInterface
public interface PinChangeListener extends PinEventListener {

    public void onValueChange(long newValue);

    @Override
    default void onValueChange(IOEvent event) {
        onValueChange(event.getValue());
    }

    @Override
    default void onModeChange(IOEvent event) {

    }
}
