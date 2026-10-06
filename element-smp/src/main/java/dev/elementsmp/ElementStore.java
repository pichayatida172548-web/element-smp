package dev.elementsmp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** เก็บธาตุของผู้เล่นต่อ world: <world>/element_smp_players.json */
public final class ElementStore {
    private final Map<UUID, Element> map = new HashMap<>();
    private Path file;
    private static final Gson GSON = new Gson();

    public void load(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("element_smp_players.json");
        map.clear();
        try {
            if (Files.exists(file)) {
                Map<String, String> raw = GSON.fromJson(Files.readString(file), new TypeToken<Map<String, String>>() {}.getType());
                if (raw != null) raw.forEach((k, v) -> Element.byId(v).ifPresent(e -> map.put(UUID.fromString(k), e)));
            }
        } catch (Exception ex) {
            ElementSmpMod.LOGGER.error("โหลดข้อมูลธาตุไม่สำเร็จ", ex);
        }
    }

    public void save() {
        if (file == null) return;
        Map<String, String> raw = new LinkedHashMap<>();
        map.forEach((k, v) -> raw.put(k.toString(), v.id()));
        try { Files.writeString(file, GSON.toJson(raw)); } catch (Exception ex) { ElementSmpMod.LOGGER.error("บันทึกข้อมูลธาตุไม่สำเร็จ", ex); }
    }

    public Element get(UUID id) { return map.get(id); }
    public void set(UUID id, Element e) { map.put(id, e); save(); }
    public void clear(UUID id) { map.remove(id); save(); }

    /** ธาตุที่มีคนใช้น้อยที่สุด (สุ่มในกลุ่ม) — เหมาะกับ Hunger Games */
    public Element randomLeastUsed(Random rnd) {
        Map<Element, Integer> count = new EnumMap<>(Element.class);
        for (Element e : Element.values()) count.put(e, 0);
        map.values().forEach(e -> count.merge(e, 1, Integer::sum));
        int min = Collections.min(count.values());
        List<Element> pool = new ArrayList<>();
        count.forEach((e, c) -> { if (c == min) pool.add(e); });
        return pool.get(rnd.nextInt(pool.size()));
    }
}
