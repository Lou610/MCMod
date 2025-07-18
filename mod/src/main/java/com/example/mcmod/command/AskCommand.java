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
        // Original ask command
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
                            answer = "I am MCMod";
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
        
        // Debug command to start haunting
        dispatcher.register(CommandManager.literal("start_haunting")
            .executes(ctx -> {
                ServerCommandSource source = ctx.getSource();
                if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                    source.sendError(Text.literal("Only players can use this command."));
                    return Command.SINGLE_SUCCESS;
                }
                
                if (HauntingManager.isHaunted(player)) {
                    player.sendMessage(Text.literal("§cYou are already being haunted!"), false);
                    return Command.SINGLE_SUCCESS;
                }
                
                HauntingManager.startHaunting(player);
                player.sendMessage(Text.literal("§4The haunting has begun... You will see the doppelganger soon."), false);
                player.sendMessage(Text.literal("§7Use /ask [question] to communicate with the doppelganger."), false);
                return Command.SINGLE_SUCCESS;
            })
        );
        
        // Debug command to stop haunting
        dispatcher.register(CommandManager.literal("stop_haunting")
            .executes(ctx -> {
                ServerCommandSource source = ctx.getSource();
                if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                    source.sendError(Text.literal("Only players can use this command."));
                    return Command.SINGLE_SUCCESS;
                }
                
                if (!HauntingManager.isHaunted(player)) {
                    player.sendMessage(Text.literal("§cYou are not being haunted."), false);
                    return Command.SINGLE_SUCCESS;
                }
                
                HauntingManager.stopHaunting();
                player.sendMessage(Text.literal("§aThe haunting has stopped."), false);
                return Command.SINGLE_SUCCESS;
            })
        );
        
        // Debug command to check haunting status
        dispatcher.register(CommandManager.literal("haunting_status")
            .executes(ctx -> {
                ServerCommandSource source = ctx.getSource();
                if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                    source.sendError(Text.literal("Only players can use this command."));
                    return Command.SINGLE_SUCCESS;
                }
                
                if (HauntingManager.isHaunted(player)) {
                    long duration = HauntingManager.getHauntingDuration(player);
                    long days = duration / 24000;
                    long hours = (duration % 24000) / 1000;
                    player.sendMessage(Text.literal("§cYou are being haunted! Duration: " + days + " days, " + hours + " hours"), false);
                } else {
                    player.sendMessage(Text.literal("§aYou are not being haunted."), false);
                }
                return Command.SINGLE_SUCCESS;
            })
        );
    }
} 