package gg.aswedrown.config;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;

@Slf4j
public final class ConfigurationLoader {

    private ConfigurationLoader() {}

    public static <T> T loadOrDefault(@NonNull String fileName,
                                      @NonNull Class<? extends T> configClass) throws IOException {
        File configFile = new File(fileName);

        if (!configFile.exists()) {
            log.warn("Missing config file {} in the working directory. " +
                            "A default configuration file will be copied from the server JAR.", fileName);

            try (InputStream in = Thread.currentThread()
                    .getContextClassLoader().getResourceAsStream(fileName)) {
                if (in == null)
                    throw new NoSuchFileException(
                            "config file not bundled in the JAR: " + fileName);

                //noinspection ResultOfMethodCallIgnored
                configFile.mkdirs();
                Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        try (Reader reader = new InputStreamReader(
                new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            return new Yaml().loadAs(reader, configClass);
        }
    }

}
