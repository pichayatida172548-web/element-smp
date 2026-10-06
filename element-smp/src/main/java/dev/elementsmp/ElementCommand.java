package dev.elementsmp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /element list
 * /element get [player]
 * /element set <player> <element>      (OP)
 * /element random <player>             (OP)  สุ่มธาตุที่มีคนใช้น้อยที่สุด
 * /element clear <player>              (OP)
 * /element reload                      (OP)  โหลด config ใหม่
 */
public final class ElementCommand {
    private ElementCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx, Commands.CommandSelection sel) {
        Predicate<CommandSourceStack> admin = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
        SuggestionProvider<CommandSourceStack> elements = (c, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Element.values()).map(Element::id), b);

        d.register(Commands.literal("element")
            .then(Commands.literal("list").executes(ElementCommand::list))
            .then(Commands.literal("get")
                .executes(c -> get(c, c.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player()).requires(admin)
                    .executes(c -> get(c, EntityArgument.getPlayer(c, "player")))))
            .then(Commands.literal("set").requires(admin)
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("element", StringArgumentType.word()).suggests(elements)
                        .executes(c -> set(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "element"))))))
            .then(Commands.literal("random").requires(admin)
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(c -> random(c, EntityArgument.getPlayer(c, "player")))))
            .then(Commands.literal("clear").requires(admin)
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(c -> clear(c, EntityArgument.getPlayer(c, "player")))))
            .then(Commands.literal("reload").requires(admin).executes(ElementCommand::reload)));
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        for (Element e : Element.values()) {
            var pair = ElementSmpMod.config.elements.get(e.id());
            Component line = Component.empty().append(e.display())
                .append(Component.literal("  " + (pair == null ? "?" : pair.pattern + " + " + pair.material)));
            c.getSource().sendSuccess(() -> line, false);
        }
        return Element.values().length;
    }

    private static int get(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        Element e = ElementSmpMod.store.get(p.getUUID());
        Component msg = e == null
            ? Component.literal(p.getGameProfile().name() + " ยังไม่มีธาตุ")
            : Component.literal(p.getGameProfile().name() + " ธาตุ: ").append(e.display());
        c.getSource().sendSuccess(() -> msg, false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> c, ServerPlayer p, String id) {
        Optional<Element> e = Element.byId(id);
        if (e.isEmpty()) { c.getSource().sendFailure(Component.literal("ไม่รู้จักธาตุ: " + id)); return 0; }
        return assign(c, p, e.get());
    }

    private static int random(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        return assign(c, p, ElementSmpMod.store.randomLeastUsed(new Random()));
    }

    private static int assign(CommandContext<CommandSourceStack> c, ServerPlayer p, Element e) {
        ElementSmpMod.store.set(p.getUUID(), e);
        TrimService.refresh(p);
        c.getSource().sendSuccess(() -> Component.literal("ตั้งธาตุของ " + p.getGameProfile().name() + " เป็น ").append(e.display()), true);
        p.sendSystemMessage(Component.literal("ธาตุของคุณคือ ").append(e.display()));
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        ElementSmpMod.store.clear(p.getUUID());
        TrimService.refresh(p);
        c.getSource().sendSuccess(() -> Component.literal("ลบธาตุของ " + p.getGameProfile().name() + " แล้ว"), true);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> c) {
        ElementSmpMod.config = ElementConfig.load();
        for (ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers()) TrimService.refresh(p);
        c.getSource().sendSuccess(() -> Component.literal("โหลด element_smp.json ใหม่แล้ว"), true);
        return 1;
    }
}
