package dev.elementsmp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/** config/element_smp.json — แก้คู่ Trim ของแต่ละธาตุได้ที่นี่ แล้วใช้ /element reload */
public final class ElementConfig {
    public int intervalTicks = 5;            // ตรวจเกราะทุกกี่ tick
    public boolean overrideExistingTrims = true; // ทับ Trim เดิมของเกราะ (คืนค่าเดิมตอนถอด)
    public boolean revertOnUnequip = true;   // ถอดเกราะแล้วคืน Trim เดิม
    public boolean abilitiesEnabled = true;  // บัฟเบา ๆ ของแต่ละธาตุ
    public boolean autoAssignRandom = false; // สุ่มธาตุให้ผู้เล่นใหม่ตอน join
    public Map<String, TrimPair> elements = new LinkedHashMap<>();

    public static final class TrimPair {
        public String pattern;
        public String material;
        public TrimPair() {}
        public TrimPair(String p, String m) { pattern = p; material = m; }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private void fillDefaults() {
        elements.putIfAbsent("fire", new TrimPair("minecraft:eye", "minecraft:redstone"));
        elements.putIfAbsent("water", new TrimPair("minecraft:coast", "minecraft:diamond"));
        elements.putIfAbsent("wind", new TrimPair("minecraft:flow", "minecraft:quartz"));
        elements.putIfAbsent("lightning", new TrimPair("minecraft:bolt", "minecraft:gold"));
        elements.putIfAbsent("ice", new TrimPair("minecraft:spire", "minecraft:diamond"));
        elements.putIfAbsent("nature", new TrimPair("minecraft:wild", "minecraft:emerald"));
        elements.putIfAbsent("earth", new TrimPair("minecraft:raiser", "minecraft:copper"));
        elements.putIfAbsent("stone", new TrimPair("minecraft:ward", "minecraft:iron"));
        elements.putIfAbsent("metal", new TrimPair("minecraft:sentry", "minecraft:iron"));
        elements.putIfAbsent("darkness", new TrimPair("minecraft:silence", "minecraft:amethyst"));
    }

    public static ElementConfig load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("element_smp.json");
        ElementConfig cfg = null;
        try {
            if (Files.exists(file)) cfg = GSON.fromJson(Files.readString(file), ElementConfig.class);
        } catch (Exception e) {
            ElementSmpMod.LOGGER.error("อ่าน config ไม่ได้ ใช้ค่าเริ่มต้นแทน", e);
        }
        if (cfg == null) cfg = new ElementConfig();
        if (cfg.elements == null) cfg.elements = new LinkedHashMap<>();
        cfg.fillDefaults();
        try { Files.writeString(file, GSON.toJson(cfg)); } catch (IOException e) { ElementSmpMod.LOGGER.error("เขียน config ไม่ได้", e); }
        return cfg;
    }
}
