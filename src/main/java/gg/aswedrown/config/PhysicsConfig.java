package gg.aswedrown.config;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

/**
 * https://github.com/AsWeDrown/awd-protocol/wiki/Physics-Config
 */
@Getter @Setter /* сеттеры нужны для snake-yaml */
public class PhysicsConfig {

    private static final transient String FILE_NAME = "config/physics.yml";

    /**
     * TODO - менять каждый раз при добавлении новых полей или удалении старых
     *        (чтобы было предупреждение о необходимости обновить файл).
     */
    private static final transient int SCHEMA_VERSION = 1;

    private int schemaVersion;

    private int   maxLag;
    private int   interpDelay;
    private int   interpBufSizeThreshold;

    private float baseEntityPlayerMs;
    private float baseEntityPlayerClimbSpeed;
    private float baseEntityPlayerW;
    private float baseEntityPlayerH;

    private float freeFallAcce;

    public static PhysicsConfig loadOrDefault() throws IOException {
        PhysicsConfig config = ConfigurationLoader
                .loadOrDefault(FILE_NAME, PhysicsConfig.class);

        if (config.getSchemaVersion() != SCHEMA_VERSION)
            throw new IllegalArgumentException(
                    "incompatible configuration file, aborting startup; file schema: "
                            + config.getSchemaVersion() + ", server schema: " + SCHEMA_VERSION);

        return config;
    }

}
