package com.mogrul.economy.commands;

import com.mogrul.economy.MogrulEconomy;
import com.mogrul.economy.utils.Config;
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

public class CurrencyCommands {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("[{}] Registering currency commands...",  MogrulEconomy.MODID);

        dispatcher.register(
                Commands.literal(Config.currencyCommandName)
                        .requires(source -> source.hasPermission(0))

                        // Member command: /currency - shows own currency.
                        .executes(context -> showCurrency(context.getSource()))

                        // Member command: /currency <target> - show players currency.
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> showOtherPlayerCurrency(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))
                        )

                        // Member subcommand: /currency send <target> <amount> - sends currency to player.
                        .then(Commands.literal("send")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> sendCurrency(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                ))
                                        )
                                )
                        )

                        // OP subcommand: /currency remove <target> <amount> - removes currency from player.
                        .then(Commands.literal("remove")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> removeCurrency(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                ))

                                        )
                                )
                        )

                        // OP subcommand: /currency set <target> <amount>
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(4))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> setCurrency(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                ))
                                        )
                                )
                        )
        );
    }

    public static int showCurrency(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        int amount = CurrencyManager.getCurrency(player);
        source.sendSuccess(() -> Component.literal("You have " + Config.currencySymbol + amount), false);
        return 1;
    }

    public static int showOtherPlayerCurrency(CommandSourceStack source, ServerPlayer target) {
        if (!(source.getEntity() instanceof ServerPlayer sender)) {
            source.sendFailure(Component.literal("Only players can send " + Config.currencyName));
            return 0;
        }

        if (sender.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.literal("You don't need to supply yourself as a target to show your " + Config.currencyName + "'s"));
            return 0;
        }

        int amount = CurrencyManager.getCurrency(target);
        source.sendSuccess(() -> Component.literal(target.getName().getString() + " has " + Config.currencySymbol + amount), false);
        return 1;
    }

    public static int sendCurrency(CommandSourceStack source, ServerPlayer target, int amount) {
        if (!(source.getEntity() instanceof ServerPlayer sender)) {
            source.sendFailure(Component.literal("Only players can send " + Config.currencyName + "."));
            return 0;
        }

        if (sender.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.literal("You can't send " + Config.currencyName + " to yourself."));
            return 0;
        }

        int senderCurrency = CurrencyManager.getCurrency(sender);

        if (senderCurrency < amount) {
            source.sendFailure(Component.literal("You don't have enough " + Config.currencyName + " to send " + amount + "\nYou have: " + senderCurrency));
            return 0;
        }

        CurrencyManager.removeCurrency(sender, amount);
        CurrencyManager.addCurrency(target, amount);

        sender.sendSystemMessage(Component.literal("You sent " + Config.currencySymbol + amount + " to " + target.getName().getString()));
        target.sendSystemMessage(Component.literal("You received " + Config.currencySymbol + amount + " from " + sender.getName().getString()));

        return 1;
    }

    public static int removeCurrency(CommandSourceStack source, ServerPlayer target, int amount) {
        if (!(source.getEntity() instanceof ServerPlayer sender)) {
            source.sendFailure(Component.literal("Only players can remove " + Config.currencyName));

            return 0;
        }

        CurrencyManager.removeCurrency(target, amount);

        sender.sendSystemMessage(Component.literal("Removed " + Config.currencySymbol + amount + " from " + target.getName().getString()), false);
        target.sendSystemMessage(Component.literal(sender.getName().getString() + " removed " + Config.currencySymbol + amount + " from you!"), false);

        return 1;
    }

    public static int setCurrency(CommandSourceStack source, ServerPlayer target, int amount) {
        if (!(source.getEntity() instanceof ServerPlayer sender)) {
            source.sendFailure(Component.literal("Only players can set " + Config.currencyName));

            return 0;
        }

        CurrencyManager.setCurrency(target, amount);

        sender.sendSystemMessage(Component.literal("Set " + target.getName().getString() + "'s " + Config.currencyName + " to " + Config.currencySymbol + amount ), false);
        target.sendSystemMessage(Component.literal(sender.getName().getString() + " set your " + Config.currencyName + " to " + Config.currencySymbol + amount), false);

        return 1;
    }
}
