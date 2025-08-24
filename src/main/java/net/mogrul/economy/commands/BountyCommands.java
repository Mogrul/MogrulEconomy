package net.mogrul.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.mogrul.economy.Colours;
import net.mogrul.economy.Config;
import net.mogrul.economy.data.BountyData;
import net.mogrul.economy.data.MemoryData;
import net.mogrul.economy.data.PlayerData;
import net.mogrul.economy.handlers.BountyDataHandler;
import net.mogrul.economy.handlers.PlayerDataHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;
import java.util.List;

import static net.mogrul.economy.MogrulEconomy.*;

@EventBusSubscriber(modid = MODID)
public class BountyCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        if (Config.bountiesEnabled) {
            registerCommands(dispatcher);
        } else {
            LOGGER.info("[{}] Bounties disabled, skipping command registration!", LOGNAME);
        }
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("[{}] Registering bounty commands!", LOGNAME);

        dispatcher.register(
                // Member base command: /bounties - Displays top 10 bounties.
                Commands.literal(Config.bountiesCommandName)
                        .executes(context -> showBountyRanks(
                                context.getSource()
                        ))

                // Member subcommand: /bounties add <target> <bounty> - Adds a bounty to a player.
                .then(Commands.literal("add")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("bounty", IntegerArgumentType.integer(Config.bountiesMinimumCost))
                                        .executes(context -> addBounty(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                IntegerArgumentType.getInteger(context, "bounty")
                                        ))
                                )
                        )
                )

                // OP subcommand: /bounties remove <target> - Removes a player's bounty.
                .then(Commands.literal("remove")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> removeBounty(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))
                        )
                )
        );

    }

    private static int addBounty(CommandSourceStack source, ServerPlayer targetPlayer, Integer bounty) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("You must be a player to use this command!"));
            return 0;
        }

        if (sourcePlayer == targetPlayer) {
            if (!sourcePlayer.getScoreboardName().equals("Dev")) {
                Component ownPlayerFailMessage = Component.literal("You can't place a bounty on yourself!");
                source.sendFailure(ownPlayerFailMessage);
                return 0;
            }
        }

        PlayerData sourceMemoryData = MemoryData.players.get(sourcePlayer.getStringUUID());
        PlayerData targetMemoryData = MemoryData.players.get(targetPlayer.getStringUUID());

        if (sourceMemoryData == null || targetMemoryData == null) {
            LOGGER.error("[{}] Failed to fetch player data from memory when adding a bounty! {} -> {}", LOGNAME, sourcePlayer, targetPlayer);
            source.sendFailure(Component.literal("Failed to fetch player data from memory!"));
            return 0;
        }

        if (bounty > sourceMemoryData.currency) {
            source.sendFailure(Component.literal("You can't afford this bounty!"));
            return 0;
        }

        // Remove bounty cost from source and apply bounty to target.
        sourceMemoryData.currency -= bounty;
        PlayerDataHandler.save(sourceMemoryData);

        BountyData targetBountyData;
        if (MemoryData.bounties.get(targetPlayer.getStringUUID()) instanceof BountyData targetBountyDataCheck) {
            targetBountyData = targetBountyDataCheck;
            targetBountyData.bounty += bounty;
            BountyDataHandler.save(targetBountyData);

        } else {
            targetBountyData = new BountyData(targetPlayer.getStringUUID(), bounty);
            BountyDataHandler.save(targetBountyData);

            MemoryData.bounties.put(targetPlayer.getStringUUID(), targetBountyData);
        }

        // Source success message.
        Component successSourceMessage = Component.literal("[")
                .append(Component.literal("Bounties")
                        .withStyle(Colours.commandTitleName
                                .withBold(true)
                        )
                )
                .append(Component.literal("]\n"))
                .append(Component.literal("Bounty of "))
                .append(Component.literal(Config.currencySymbol + bounty)
                        .withStyle(Colours.currencyName
                                .withBold(true)
                        )
                )
                .append(Component.literal(" added to "))
                .append(Component.literal(targetMemoryData.name)
                        .withStyle(Colours.playerName)
                );

        // If their bounty already exists, append bounty increased to message.
        if (targetBountyData.bounty != bounty) {
            successSourceMessage = successSourceMessage.copy()
                    .append(Component.literal("\nIncreasing to "))
                    .append(Component.literal(Config.currencySymbol + targetBountyData.bounty)
                            .withStyle(Colours.currencyName)
                    );
        }

        source.sendSystemMessage(successSourceMessage);

        // Target bounty message.
        Component successTargetMessage = Component.literal("[")
                .append(Component.literal("Bounties")
                        .withStyle(Colours.commandTitleName
                                .withBold(true)
                        )
                )
                .append(Component.literal("]\n"))
                .append(Component.literal("Your bounty has increased to "))
                .append(Component.literal(Config.currencySymbol + targetBountyData.bounty)
                        .withStyle(Colours.currencyName
                                .withBold(true)
                        )
                )
                .append(Component.literal("!"));

        targetPlayer.sendSystemMessage(successTargetMessage);

        // Log messages.
        LOGGER.info("[{}] {} added a bounty of {} to {}", LOGNAME, sourceMemoryData.name, bounty, targetMemoryData.name);

        return 1;
    }

    private static int showBountyRanks(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendFailure(Component.literal("You must be203." +
                    ".24 a player to use this command!"));
            return 0;
        }

        List<BountyData> playersBountyData = BountyDataHandler.loadAll();
        playersBountyData.sort(Comparator.comparing((BountyData b) -> b.bounty).reversed());

        Component successMessage = Component.literal("[")
                .append(Component.literal(Config.bountiesCommandName)
                        .withStyle(Colours.commandTitleName
                                .withBold(true)
                        )
                )
                .append(Component.literal("]\n"));

        if (playersBountyData.isEmpty()) {
            source.sendFailure(Component.literal("There are no bounties to show!"));
            return 0;
        }

        int index = 1;
        for (BountyData bountyData : playersBountyData) {
            if (index > 10) break;
            PlayerData playerData = MemoryData.players.get(bountyData.playerUUID);
            if (playerData == null) continue;

            successMessage = successMessage.copy()
                    .append(Component.literal(index + ". "))
                    .append(Component.literal(playerData.name)
                            .withStyle(Colours.playerName)
                    )
                    .append(Component.literal(" - "))
                    .append(Component.literal(Config.currencySymbol + bountyData.bounty)
                            .withStyle(Colours.currencyName
                                    .withBold(true)
                            )
                    );

            index++;
        }

        source.sendSystemMessage(successMessage);

        return 1;
    }

    private static int removeBounty(CommandSourceStack source, ServerPlayer targetPlayer) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendFailure(Component.literal("You must be a player to use this command!"));
            return 0;
        }

        if (!(MemoryData.bounties.get(targetPlayer.getStringUUID()) instanceof BountyData bountyData)) {
            source.sendFailure(Component.literal("This player doesn't have a bounty!"));
            return 0;
        }

        Component successMessage = Component.literal("[")
                .append(Component.literal(Config.bountiesCommandName)
                        .withStyle(Colours.commandTitleName)
                )
                .append(Component.literal("]\n"))
                .append(Component.literal("Removed the bounty of "))
                .append(Component.literal(targetPlayer.getScoreboardName())
                        .withStyle(Colours.playerName
                                .withBold(true)
                        )
                );

        source.sendSystemMessage(successMessage);

        // Remove the bounty
        BountyDataHandler.remove(bountyData);
        MemoryData.bounties.remove(targetPlayer.getStringUUID());

        return 1;
    }
}
