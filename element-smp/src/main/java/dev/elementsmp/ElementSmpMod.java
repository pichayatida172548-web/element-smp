package dev.elementsmp;

import java.util.Random;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementSmpMod implements ModInitializer {
    public static final String MOD_ID = "element_smp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ElementConfig config;
    public static final ElementStore store = new ElementStore();

    @Override
    public void onInitialize() {
        config = ElementConfig.load();

        ServerLifecycleEvents.SERVER_STARTED.register(store::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // คืน Trim เดิมก่อนบันทึกโลก
            for (ServerPlayer p : server.getPlayerList().getPlayers()) TrimService.revertAll(p);
            store.save();
        });

        ServerTickEvents.END_SERVER_TICK.register(ElementSmpMod::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            if (config.autoAssignRandom && store.get(p.getUUID()) == null) {
                store.set(p.getUUID(), store.randomLeastUsed(new Random()));
            }
            TrimService.refresh(p);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TrimService.revertAll(handler.getPlayer()));

        // เกราะที่ดรอป/ตาย -> คืน Trim เดิมทันที
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity ie) TrimService.revert(ie.getItem(), world.registryAccess());
        });

        CommandRegistrationCallback.EVENT.register(ElementCommand::register);
        LOGGER.info("Element SMP loaded");
    }

    private static void tick(MinecraftServer server) {
        int t = server.getTickCount();
        boolean trim = t % Math.max(1, config.intervalTicks) == 0;
        boolean abil = config.abilitiesEnabled && t % 20 == 0;
        if (!trim && !abil) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (trim) TrimService.refresh(p);
            if (abil) Abilities.tick(p);
        }
    }
}
