package selector;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.javatuples.Triplet;

import java.nio.charset.Charset;
import java.util.List;

public interface VMInterface {

    @SneakyThrows
    void validateVMNames(List<String> names);

    @SneakyThrows
    void checkInstallation();

    @SneakyThrows
    void switchToVM(SwitchEntry to);

    @SneakyThrows
    default Triplet<String, String, Integer> runAndGet(String... params) {
        final Process process = new ProcessBuilder(params).start();
        process.waitFor();
        return new Triplet<>(IOUtils.toString(process.getInputStream(), Charset.defaultCharset()), IOUtils.toString(process.getErrorStream(), Charset.defaultCharset()), process.exitValue());
    }

    @SneakyThrows
    default int runAndForward(String... params) {
        final Process process = new ProcessBuilder(params).inheritIO().start();
        process.waitFor();
        return process.exitValue();
    }
}
