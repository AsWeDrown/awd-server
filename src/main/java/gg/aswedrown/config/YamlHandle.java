package gg.aswedrown.config;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.Map;

@RequiredArgsConstructor
public class YamlHandle {

    @NonNull
    private final Map<String, Object> globalSection;

    public <T> T get(String key) {
        return get(key, null);
    }

    public <T> T require(String key) {
        T result = get(key);

        if (result == null)
            throw new YAMLException("missing configuration key: " + key);

        return result;
    }

    public <T> T get(@NonNull String key, T def) {
        String[] fqKeyPath = key.split("\\.");
        Map<String, Object> lastInnerSection = resolveFinalInnerSection(fqKeyPath);
        return lastInnerSection == null ? def
                : (T) lastInnerSection.getOrDefault(fqKeyPath[fqKeyPath.length - 1], def);
    }

    private Map<String, Object> resolveFinalInnerSection(String[] fqKeyPath) {
        Map<String, Object> lastInnerSection = globalSection;

        for (int i = 0; i < fqKeyPath.length - 1; i++) {
            try {
                Map<String, Object> innerSection
                        = (Map<String, Object>) lastInnerSection.get(fqKeyPath[i]);

                if (innerSection != null)
                    lastInnerSection = innerSection;
                else
                    return null;
            } catch (ClassCastException ex) {
                return null;
            }
        }

        return lastInnerSection;
    }

}
