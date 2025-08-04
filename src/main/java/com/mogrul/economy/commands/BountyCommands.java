package com.mogrul.economy.commands;

import com.mogrul.economy.MogrulEconomy;
import com.mogrul.economy.utils.Config;
import com.mogrul.economy.utils.database.BountyManager;
import com.mogrul.economy.utils.database.CurrencyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public class BountyCommands {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("[{}] Registering bounty commands...", MogrulEconomy.MODID);

        dispatcher.register(
                Commands.literal(Config.bountyCommandName)
                .requires(source -> source.hasPermission(0))

                // Member command: /bounty set <player> <amount>
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.player())  // Argument for player target
                                .then(Commands.argument("price", IntegerArgumentType.integer(1))  // Argument for price
                                        .executes(context -> addBounty(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                IntegerArgumentType.getInteger(context, "price")
                                        ))
                                )
                        )
                )

                // OP command: /bounty remove <player>
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

    public static int addBounty(CommandSourceStack source, ServerPlayer target, Integer price) {
        ServerPlayer sender = source.getPlayer();

        assert sender != null;
        if (!sender.getName().getString().equals("Dev")) {
            if (sender.getStringUUID().equals(target.getStringUUID())) {
                source.sendFailure(Component.literal("You can't put a bounty on yourself!"));

                return 0;
            }
        }

        int senderCurrency = CurrencyManager.getCurrency(sender);
        if (price > senderCurrency) {
            source.sendFailure(Component.literal("You have " + Config.currencySymbol + String.format("%,d", senderCurrency) + " - you can't afford a " + Config.currencySymbol + String.format("%,d", price) + " bounty!"));

            return 0;
        }

        CurrencyManager.removeCurrency(sender, price);

        BountyManager.addBounty(target, price);
        int currentBounty = BountyManager.getBounty(target);

        source.sendSuccess(() -> Component.literal( Config.currencySymbol + String.format("%,d", price) + " bounty added to " + target.getName().getString()), true);
        target.sendSystemMessage(Component.literal("Your bounty has increased to " + Config.currencySymbol + String.format("%,d", currentBounty) + "!"));

        return 1;
    }

    public static int removeBounty(CommandSourceStack source, ServerPlayer target) {
        int currentBounty = BountyManager.getBounty(target);

        if (currentBounty <= 0) {
            source.sendFailure(Component.literal(target.getName().getString() + "has no bounty to be removed!"));
            return 0;
        }

        BountyManager.removeBounty(target);

        source.sendSuccess(() -> Component.literal("Removed " + target.getName().getString() + " bounty of " + Config.currencySymbol + String.format("%,d", currentBounty)), true);
        target.sendSystemMessage(Component.literal("Your bounty of " + Config.currencySymbol + String.format("%,d", currentBounty) + " has been removed!"));

        return 1;
    }

    public static void onBountyComplete(ServerPlayer killerPlayer, ServerPlayer victimPlayer) {
        int victimBounty = BountyManager.getBounty(victimPlayer);
        BountyManager.removeBounty(victimPlayer);

        killerPlayer.sendSystemMessage(Component.literal("You claimed a bounty of " + Config.currencySymbol + String.format("%,d", victimBounty) + " for killing " + victimPlayer.getName().getString()));
        victimPlayer.sendSystemMessage(Component.literal("Your bounty of " + Config.currencySymbol + String.format("%,d", victimBounty) + " was claimed by " + killerPlayer.getName().getString()));
    }
}
