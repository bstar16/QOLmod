package com.bstar.qolmod.command;

import com.bstar.qolmod.feature.impl.StorageLabelsFeature;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.concurrent.CompletableFuture;

public final class QOLmodClientCommands {
    private QOLmodClientCommands() {
    }

    public static void register(StorageLabelsFeature storageLabelsFeature) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("qol")
                        .then(ClientCommandManager.literal("storage")
                                .then(ClientCommandManager.literal("add")
                                        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    String name = StringArgumentType.getString(context, "name").trim();
                                                    if (name.isEmpty()) {
                                                        context.getSource().sendError(Text.literal("Storage label name cannot be empty."));
                                                        return 0;
                                                    }

                                                    storageLabelsFeature.beginPlacement(context.getSource().getClient(), name);
                                                    return 1;
                                                })))
                                .then(ClientCommandManager.literal("cancel")
                                        .executes(context -> {
                                            storageLabelsFeature.cancelPlacement(context.getSource().getClient(), true);
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("edit")
                                        .executes(context -> {
                                            storageLabelsFeature.openEditor(context.getSource().getClient());
                                            return 1;
                                        })
                                        .then(ClientCommandManager.argument("identifier", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> suggestLabelNames(storageLabelsFeature, context, builder))
                                                .executes(context -> {
                                                    storageLabelsFeature.openEditor(
                                                            context.getSource().getClient(),
                                                            StringArgumentType.getString(context, "identifier"),
                                                            false
                                                    );
                                                    return 1;
                                                })))
                                .then(ClientCommandManager.literal("icon")
                                        .executes(context -> {
                                            storageLabelsFeature.openEditor(context.getSource().getClient());
                                            return 1;
                                        })
                                        .then(ClientCommandManager.argument("identifier", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> suggestLabelNames(storageLabelsFeature, context, builder))
                                                .executes(context -> {
                                                    storageLabelsFeature.openEditor(
                                                            context.getSource().getClient(),
                                                            StringArgumentType.getString(context, "identifier"),
                                                            false
                                                    );
                                                    return 1;
                                                })))
                                .then(ClientCommandManager.literal("color")
                                        .executes(context -> {
                                            storageLabelsFeature.openEditor(context.getSource().getClient());
                                            return 1;
                                        })
                                        .then(ClientCommandManager.argument("identifier", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> suggestLabelNames(storageLabelsFeature, context, builder))
                                                .executes(context -> {
                                                    storageLabelsFeature.openEditor(
                                                            context.getSource().getClient(),
                                                            stripDeprecatedColorSuffix(StringArgumentType.getString(context, "identifier")),
                                                            true
                                                    );
                                                    return 1;
                                                })))
                                .then(ClientCommandManager.literal("remove")
                                        .executes(context -> {
                                            storageLabelsFeature.removeLookedAt(context.getSource().getClient());
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("list")
                                        .executes(context -> {
                                            storageLabelsFeature.listNearby(context.getSource().getClient());
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("clear-nearby")
                                        .executes(context -> {
                                            storageLabelsFeature.clearNearby(context.getSource().getClient());
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("debug")
                                        .executes(context -> {
                                            storageLabelsFeature.debug(context.getSource().getClient());
                                            return 1;
                                        })))));
    }

    private static CompletableFuture<Suggestions> suggestLabelNames(
            StorageLabelsFeature storageLabelsFeature,
            CommandContext<FabricClientCommandSource> context,
            SuggestionsBuilder builder
    ) {
        return CommandSource.suggestMatching(
                storageLabelsFeature.labelNamesInCurrentDimension(context.getSource().getClient()).stream()
                        .map(QOLmodClientCommands::quoteIfNeeded),
                builder
        );
    }

    private static String quoteIfNeeded(String value) {
        if (value.indexOf(' ') < 0) {
            return value;
        }

        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String stripDeprecatedColorSuffix(String identifier) {
        String trimmed = identifier.trim();
        int lastSpace = trimmed.lastIndexOf(' ');
        if (lastSpace <= 0) {
            return trimmed;
        }

        String possibleColor = trimmed.substring(lastSpace + 1);
        return isColorLike(possibleColor) ? trimmed.substring(0, lastSpace).trim() : trimmed;
    }

    private static boolean isColorLike(String value) {
        String normalized = value.toLowerCase();
        return switch (normalized) {
            case "white", "yellow", "aqua", "green", "red", "purple", "gold", "gray", "grey", "blue" -> true;
            default -> normalized.matches("#?[0-9a-f]{6}") || normalized.matches("0x[0-9a-f]{6}");
        };
    }

    public static void sendUnavailable(MinecraftClient client) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("[QOLmod] ").formatted(Formatting.GRAY)
                    .append(Text.literal("Storage Labels is not available.").formatted(Formatting.RED)), false);
        }
    }
}
