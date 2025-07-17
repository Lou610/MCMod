package com.example.mcmod.command;

import com.example.mcmod.HauntingManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;

public class AskCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ask")
            .then(CommandManager.argument("question", StringArgumentType.greedyString())
                .executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                        source.sendError(Text.literal("Only players can use this command."));
                        return Command.SINGLE_SUCCESS;
                    }
                    if (!HauntingManager.isHaunted(player)) {
                        player.sendMessage(Text.literal("You feel nothing but silence..."), false);
                        return Command.SINGLE_SUCCESS;
                    }
                    String question = StringArgumentType.getString(ctx, "question").trim().toLowerCase().replaceAll("[^a-z0-9 ]", "");
                    player.sendMessage(Text.literal("§7You ask: '" + StringArgumentType.getString(ctx, "question") + "'"), false);
                    String answer;
                    switch (question) {
                        case "who are you":
                            answer = "I am MeeperCreeper";
                            break;
                        case "where are you":
                            answer = "Where I can see you but you cannot see me...";
                            break;
                        case "what are you":
                            answer = "I am you...";
                            break;
                        default:
                            answer = "The Doppelgänger whispers something you cannot understand...";
                            break;
                    }
                    player.sendMessage(Text.literal("§8The Doppelgänger whispers: '" + answer + "'"), false);
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }
} 