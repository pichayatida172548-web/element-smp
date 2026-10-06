package dev.elementsmp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * บัฟเบา ๆ แบบมีเงื่อนไข (ไม่ใช่บัฟถาวร) เพื่อให้สมดุลใน SMP / Hunger Games
 * ทุกบัฟเป็น amplifier 0 และต้องอยู่ในสถานการณ์ที่กำหนดเท่านั้น ปรับ/ปิดได้ใน config (abilitiesEnabled)
 */
public final class Abilities {
    private Abilities() {}

    public static void tick(ServerPlayer p) {
        Element e = ElementSmpMod.store.get(p.getUUID());
        if (e == null || p.isSpectator() || !p.isAlive()) return;
        ServerLevel level = p.level();
        BlockPos pos = p.blockPosition();
        boolean sky = level.canSeeSky(pos);

        switch (e) {
            case FIRE -> { if (p.isOnFire() || p.isInLava()) give(p, MobEffects.FIRE_RESISTANCE, 60); }
            case WATER -> { if (p.isInWater()) { give(p, MobEffects.WATER_BREATHING, 60); give(p, MobEffects.DOLPHINS_GRACE, 60); } }
            case WIND -> {
                if (sky) give(p, MobEffects.JUMP_BOOST, 60);
                if (!p.onGround() && p.isShiftKeyDown()) give(p, MobEffects.SLOW_FALLING, 60);
            }
            case EARTH -> { if (!sky && p.getY() < 50) give(p, MobEffects.HASTE, 60); }
            case LIGHTNING -> { if (level.isRaining()) give(p, MobEffects.SPEED, 60); }
            case ICE -> { if (level.getBlockState(pos.below()).is(BlockTags.ICE)) give(p, MobEffects.SPEED, 60); }
            case NATURE -> {
                if (p.isShiftKeyDown() && level.getBlockState(pos.below()).is(BlockTags.DIRT)) give(p, MobEffects.REGENERATION, 60);
            }
            case STONE -> { if (p.isShiftKeyDown()) give(p, MobEffects.RESISTANCE, 60); }
            case METAL -> { if (p.getHealth() <= p.getMaxHealth() * 0.3f) give(p, MobEffects.RESISTANCE, 60); }
            case DARKNESS -> { if (level.getMaxLocalRawBrightness(pos) <= 4) give(p, MobEffects.NIGHT_VISION, 260); }
        }
    }

    private static void give(ServerPlayer p, Holder<MobEffect> effect, int duration) {
        p.addEffect(new MobEffectInstance(effect, duration, 0, true, false, false));
    }
}
