package dev.elementsmp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

/**
 * หัวใจของ mod: แก้ "เฉพาะ component TRIM" บน ItemStack ตัวเดิมที่ผู้เล่นใส่อยู่ (in-place)
 * ไม่สร้างไอเทมใหม่ จึงไม่กระทบ enchant / ชื่อ / durability / component อื่น ๆ
 *
 * ก่อนทับ จะจำ Trim เดิม (ถ้ามี) ไว้ใน custom_data ชั่วคราว แล้วคืนให้ตอนถอดเกราะ
 */
public final class TrimService {
    private static final String KEY = "element_smp";
    private static final EquipmentSlot[] ARMOR = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private TrimService() {}

    public static void refresh(ServerPlayer p) {
        ElementConfig cfg = ElementSmpMod.config;
        Element element = ElementSmpMod.store.get(p.getUUID());
        RegistryAccess access = p.level().registryAccess();

        List<ItemStack> worn = new ArrayList<>(4);
        for (EquipmentSlot slot : ARMOR) {
            ItemStack s = p.getItemBySlot(slot);
            if (!s.isEmpty()) worn.add(s);
        }

        // 1) ใส่ / เปลี่ยนเกราะ / เปลี่ยนธาตุ -> ใส่ Trim ของธาตุ
        Optional<ArmorTrim> desired = element == null ? Optional.empty() : desiredTrim(access, element);
        for (ItemStack s : worn) {
            if (desired.isPresent() && s.is(ItemTags.TRIMMABLE_ARMOR)) apply(s, desired.get(), access, cfg);
            else if (isMarked(s)) revert(s, access); // ไม่มีธาตุแล้ว
        }

        // 2) ถอดเกราะ -> คืน Trim เดิมของชิ้นที่ไม่ได้ใส่อยู่แล้ว
        if (cfg.revertOnUnequip) {
            for (Slot slot : p.containerMenu.slots) revertIfNotWorn(slot.getItem(), worn, access);
            for (Slot slot : p.inventoryMenu.slots) revertIfNotWorn(slot.getItem(), worn, access);
            revertIfNotWorn(p.containerMenu.getCarried(), worn, access);
        }
    }

    public static void revertAll(ServerPlayer p) {
        RegistryAccess access = p.level().registryAccess();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) revert(p.getInventory().getItem(i), access);
        for (EquipmentSlot slot : EquipmentSlot.values()) revert(p.getItemBySlot(slot), access);
        revert(p.containerMenu.getCarried(), access);
    }

    private static void revertIfNotWorn(ItemStack s, List<ItemStack> worn, RegistryAccess access) {
        if (s.isEmpty() || !isMarked(s)) return;
        for (ItemStack w : worn) if (w == s) return; // ยังใส่อยู่
        revert(s, access);
    }

    // ---------------------------------------------------------------- apply

    public static Optional<ArmorTrim> desiredTrim(RegistryAccess access, Element e) {
        ElementConfig.TrimPair pair = ElementSmpMod.config.elements.get(e.id());
        if (pair == null) return Optional.empty();
        try {
            ResourceKey<TrimPattern> pk = ResourceKey.create(Registries.TRIM_PATTERN, Identifier.parse(pair.pattern));
            ResourceKey<TrimMaterial> mk = ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.parse(pair.material));
            var pattern = access.lookupOrThrow(Registries.TRIM_PATTERN).get(pk);
            var material = access.lookupOrThrow(Registries.TRIM_MATERIAL).get(mk);
            if (pattern.isPresent() && material.isPresent()) return Optional.of(new ArmorTrim(material.get(), pattern.get()));
            ElementSmpMod.LOGGER.warn("ไม่พบ trim pattern/material ของธาตุ {}: {} / {}", e.id(), pair.pattern, pair.material);
        } catch (Exception ex) {
            ElementSmpMod.LOGGER.warn("config trim ของธาตุ {} ไม่ถูกต้อง", e.id(), ex);
        }
        return Optional.empty();
    }

    private static void apply(ItemStack stack, ArmorTrim desired, RegistryAccess access, ElementConfig cfg) {
        ArmorTrim current = stack.get(DataComponents.TRIM);
        if (current != null && current.equals(desired)) return;          // ตรงอยู่แล้ว
        boolean marked = isMarked(stack);
        if (current != null && !marked && !cfg.overrideExistingTrims) return;

        if (!marked) { // จำของเดิมครั้งแรกครั้งเดียว
            Tag orig = null;
            if (current != null) {
                orig = ArmorTrim.CODEC.encodeStart(access.createSerializationContext(NbtOps.INSTANCE), current).result().orElse(null);
            }
            CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag root = cd == null ? new CompoundTag() : cd.copyTag();
            CompoundTag mark = new CompoundTag();
            if (orig != null) mark.put("orig", orig);
            root.put(KEY, mark);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
        stack.set(DataComponents.TRIM, desired); // เปลี่ยนเฉพาะ TRIM
    }

    // --------------------------------------------------------------- revert

    public static boolean isMarked(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        return cd != null && cd.copyTag().contains(KEY);
    }

    public static void revert(ItemStack stack, RegistryAccess access) {
        if (!isMarked(stack)) return;
        CompoundTag root = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        Tag orig = root.getCompound(KEY).map(c -> c.get("orig")).orElse(null);
        root.remove(KEY);

        if (orig == null) {
            stack.remove(DataComponents.TRIM);
        } else {
            ArmorTrim.CODEC.parse(access.createSerializationContext(NbtOps.INSTANCE), orig).result()
                .ifPresentOrElse(t -> stack.set(DataComponents.TRIM, t), () -> stack.remove(DataComponents.TRIM));
        }
        if (root.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
