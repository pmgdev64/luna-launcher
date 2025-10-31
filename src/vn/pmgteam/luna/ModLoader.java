package vn.pmgteam.luna;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

public class ModLoader {
    public static List<ModInfo> loadMods() {
        try (InputStream is = ModLoader.class.getResourceAsStream("/data/config.json")) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(is, new TypeReference<List<ModInfo>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
