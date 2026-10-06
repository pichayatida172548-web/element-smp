package dev.elementsmp;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum Element {
    FIRE("fire", "Fire", "ไฟ", ChatFormatting.RED),
    WATER("water", "Water", "น้ำ", ChatFormatting.AQUA),
    WIND("wind", "Wind", "ลม", ChatFormatting.WHITE),
    EARTH("earth", "Earth", "ดิน", ChatFormatting.GOLD),
    LIGHTNING("lightning", "Lightning", "สายฟ้า", ChatFormatting.YELLOW),
    ICE("ice", "Ice", "น้ำแข็ง", ChatFormatting.BLUE),
    NATURE("nature", "Nature", "ธรรมชาติ", ChatFormatting.GREEN),
    STONE("stone", "Stone", "หิน", ChatFormatting.GRAY),
    METAL("metal", "Metal", "โลหะ", ChatFormatting.DARK_AQUA),
    DARKNESS("darkness", "Darkness", "ความมืด", ChatFormatting.DARK_PURPLE);

    private final String id, english, thai;
    private final ChatFormatting color;

    Element(String id, String english, String thai, ChatFormatting color) {
        this.id = id; this.english = english; this.thai = thai; this.color = color;
    }

    public String id() { return id; }

    public Component display() {
        return Component.literal(english + " (" + thai + ")").withStyle(color);
    }

    public static Optional<Element> byId(String s) {
        for (Element e : values()) if (e.id.equalsIgnoreCase(s)) return Optional.of(e);
        return Optional.empty();
    }
}
