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
import net.mogrul.economy.data.MemoryData;
import net.mogrul.economy.data.PlayerData;
import net.mogrul.economy.handlers.PlayerDataHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import static net.mogrul.economy.MogrulEconomy.*;

@EventBusSubscriber(modid = MODID)
public class CurrencyCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        registerCommands(dispatcher);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("[{}] Registering currency commands!", LOGNAME);

        // Register base command
        dispatcher.register(
                Commands.literal(Config.currencyCommandName)
                        .requires(source -> source.hasPermission(0))

                        // Member command: /currency - shows own currency.
                        .executes(context -> showCurrency(
                                context.getSource()
                        ))

                        // Member subcommand: /currency <target> - Shows target's currency.
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> showPlayerCurrency(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))
                        )

                        // Member subcommand: /currency send <target> <amount> - Sends currency to a player.
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

                        // OP subcommand: /currency remove <target> <amount> - Removes currency from a player.
                        .then(Commands.literal("remove")
                                .requires(source -> source.hasPermission(4))
                                .then(Commands.argument("target",  EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> removeCurrency(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                ))
                                        )
                                )
                        )

                        // OP subcommand: /currency set <target> <amount> - Sets currency from a player.
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(4))
                                .then(Commands.argument("target",  EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> setCurrency(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                ))
                                        )
                                )
                        )

                        // OP subcommand: /currency add <target> <amount> - Adds to a players currency.
                        .then(Commands.literal("add")
                                .requires(source -> source.hasPermission(4))
                                .then(Commands.argument("target",  EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> addCurrency(
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
        // Check if is ServerPlayer
        if (!(source.getEntity() instanceof ServerPlayer serverPlayer)) {
            source.sendFailure(Component.literal("You must be a player to use this command!"));
            return 0;
        }

        PlayerData playerData = MemoryData.players.get(serverPlayer.getStringUUID());
        Component successMessage = Component.literal("[")
                .append(Component.literal(Config.currencyNamePlural)
                        .withStyle(hexToTextColorStyle(Colours.currencyName)
                                .withBold(true)
                        )
                )
                .append(Component.literal("]\n"))
                .append(Component.literal(playerData.name)
                        .withStyle(hexToTextColorStyle(Colours.playerName)
                                .withBold(true)
                        )
                )
                .append(Component.literal(" -> "))
                .append(Component.literal(Config.currencySymbol + String.format("%,d", playerData.currency))
                        .withStyle(hexToTextColorStyle(Colours.currencyName)
                                .withBold(true)
                        )
                );

        LOGGER.info("[{}] {} requested to see their {}!", LOGNAME, playerData.name, Config.currencyNamePlural);

        source.sendSuccess(() -> successMessage, false);
        return 1;
    }

    public static int showPlayerCurrency(CommandSourceStack source, ServerPlayer targetPlayer) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("You must be a player to use this command!"));
            return 0;
        }

        if (sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
            return showCurrency(source);
        }

        PlayerData playerData = MemoryData.players.get(targetPlayer.getStringUUID());
        Component successMessage = Component.literal("[")
                        .append(Component.literal(Config.currencyNamePlural)
                                .withStyle(hexToTextColorStyle(Colours.currencyName)
                                        .withBold(true)
                                )
                        )
                        .append(Component.literal("]\n"))
                        .append(Component.literal(playerData.name)
                                .withStyle(hexToTextColorStyle(Colours.playerName)
                                        .withBold(true)
                                )
                        )
                        .append(Component.literal(" -> "))
                        .append(Component.literal(Config.currencySymbol + String.format("%,d", playerData.currency))
                                .withStyle(hexToTextColorStyle(Colours.currencyName)
                                        .withBold(true)
                                )
                        );

        source.sendSuccess(() -> successMessage, false);

        LOGGER.info("[{}] {} requested {}'s {}!",
                LOGNAME,
                targetPlayer.getName().getString(),
                playerData.name,
                Config.currencySymbol
        );

        return 1;
    }

    public static int sendCurrency(CommandSourceStack source, ServerPlayer targetPlayer, int amount) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("You must be a player to use this command!"));
            return 0;
        }

        if (!sourcePlayer.getName().getString().equals("Dev")) {
            if (sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
                Component ownPlayerFailureMessage = Component.literal("You can't send ")
                        .append(Component.literal(Config.currencyNamePlural)
                                .withStyle(hexToTextColorStyle(Colours.currencyName)
                                        .withBold(true)
                                )
                        )
                        .append(Component.literal(" to yourself!"));

                source.sendFailure(ownPlayerFailureMessage);
                return 0;
            }
        }

        PlayerData sourceData = MemoryData.players.get(targetPlayer.getStringUUID());
        if (amount > sourceData.currency) {
            Component notEnoughFailMessage = Component.literal("You don't have enough to send ")
                    .append(Component.literal(Config.currencySymbol + amount)
                            .withStyle(hexToTextColorStyle(Colours.currencyName)
                                    .withBold(true)
                            )
                    )
                    .append(Component.literal(" you have "))
                    .append(Component.literal(Config.currencySymbol + String.format("%,d", sourceData.currency))
                            .withStyle(hexToTextColorStyle(Colours.currencyName)
                                    .withBold(true)
                            )
                    );

            source.sendFailure(notEnoughFailMessage);
            return 0;
        }

        // Remove from player.
        sourceData.currency -= amount;
        PlayerDataHandler.save(sourceData);

        // Add to player.
        PlayerData targetMemoryData = MemoryData.players.get(targetPlayer.getStringUUID());
        targetMemoryData.currency += amount;
        PlayerDataHandler.save(targetMemoryData);

        Component successMessage = Component.literal("[")
                .append(Component.literal(Config.currencyNamePlural)
                        .withStyle(hexToTextColorStyle(Colours.currencyName)
                                .withBold(true)
                        )
                )
                .append(Component.literal("]\n"))
                .append(Component.literal(sourceData.name)
                        .withStyle(hexToTextColorStyle(Colours.playerName)
                                .withBold(true)
                        )
                )
                .append(Component.literal(" " + Config.currencySymbol + String.format("%,d", sourceData.currency))
                        .withStyle(hexToTextColorStyle(Colours.currencyName)
                                .withBold(true)
                        )
                )
                .append(Component.literal(" -> "))
                .append(Component.literal(targetMemoryData.name)
                        .withStyle(hexToTextColorStyle(Colours.playerName)
                                .withBold(true)
                        )
                );

        source.sendSuccess(() -> successMessage, false);
        targetPlayer.sendSystemMessage(successMessage);

        LOGGER.info("[{}] {} sent {}{} to {}!",
                LOGNAME,
                sourceData.name,
                Config.currencySymbol,
                amount,
                targetMemoryData.name
        );

        return 1;
    }

    public static int removeCurrency(CommandSourceStack source, ServerPlayer targetPlayer, int amount) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("You must be a player to use this command!"));
            return 0;
        }

        PlayerData targetData = MemoryData.players.get(targetPlayer.getStringUUID());
        targetData.currency = Math.max(0, targetData.currency - amount);
        PlayerDataHandler.save(targetData);

        Component successMessage = Component.literal("[")
                .append(Component.literal(Config.currencyNamePlural)
                        .withStyle(hexToTextColorStyle(Colours.currencyName)
                                .withBold(true)
                        )
                )
                .append(Component.literal("]\n"))
                .append(Component.literal(sourcePlayer.getName().getString())
                        .withStyle(hexToTextColorStyle(Colours.playerName)
                                .withBold(true)
                        )
                )
                .append(Component.literal(" x "))
                .append(Component.literal(Config.currencySymbol + String.format("%,d", amount) + " ")
                        .withStyle(hexToTextColorStyle(Colours.currencyName)
                                .withBold(true)
                        )
                )
                .append(Component.literal(targetPlayer.getName().getString())
                        .withStyle(hexToTextColorStyle(Colours.playerName)
                                .withBold(true)
                        )
                );

        source.sendSuccess(() -> successMessage, false);
        targetPlayer.sendSystemMessage(successMessage);

        LOGGER.info("[{}] {} removed {}{} from {}!",
                LOGNAME,
                sourcePlayer.getName().getString(),
                Config.currencySymbol,
                amount,
                targetData.name
        );

        return 1;
    }

    public static int setCurrency(CommandSourceStack source, ServerPlayer targetPlayer, int amount) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("You must be a player to use this command!"));
            return 0;
        }

        PlayerData targetData = MemoryData.players.get(targetPlayer.getStringUUID());
        targetData.currency = amount;
        PlayerDataHandler.save(targetData);

        Component successMessage = Component.literal("[")
                .append(Component.literal(Config.currencyNamePlural)
                        .withStyle(hexToTextColorStyle(Colours.currencyName)
                            .withBold(true)
                        )
                )
                .append(Component.literal("]\n"))
                .append(Component.literal(sourcePlayer.getName().getString())
                        .withStyle(hexToTextColorStyle(Colours.playerName)
                                .withBold(true)
                        )
                )
                .append(Component.literal(" == "))
                .append(Component.literal(Config.currencySymbol + String.format("%,d", targetData.currency) + " ")
                        .withStyle(hexToTextColorStyle(Colours.currencyName)
                                .withBold(true)
                        )
                )
                .append(Component.literal(targetPlayer.getName().getString())
                        .withStyle(hexToTextColorStyle(Colours.playerName)
                                .withBold(true)
                        )
                );

        source.sendSuccess(() -> successMessage, false);
        targetPlayer.sendSystemMessage(successMessage);

        LOGGER.info("[{}] {} set {}'s currency to {}{}",
                LOGNAME,
                sourcePlayer.getName().getString(),
                targetData.name,
                Config.currencySymbol,
                amount
        );

        return 1;
    }

    public static int addCurrency(CommandSourceStack source, ServerPlayer targetPlayer, int amount) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("You must be a player to use this command!"));
            return 0;
        }

        PlayerData targetData = MemoryData.players.get(targetPlayer.getStringUUID());
        targetData.currency += amount;
        PlayerDataHandler.save(targetData);

        Component successMessage = Component.literal("[")
                .append(Component.literal(Config.currencyNamePlural)
                        .withStyle(hexToTextColorStyle(Colours.currencyName)
                                .withBold(true)
                        )
                )
                .append(Component.literal("]\n"))
                .append(Component.literal(sourcePlayer.getName().getString())
                        .withStyle(hexToTextColorStyle(Colours.playerName)
                                .withBold(true)
                        )
                )
                .append(Component.literal(" += "))
                .append(Component.literal(Config.currencySymbol + String.format("%,d", targetData.currency) + " ")
                        .withStyle(hexToTextColorStyle(Colours.currencyName)
                                .withBold(true)
                        )
                )
                .append(Component.literal(targetPlayer.getName().getString())
                        .withStyle(hexToTextColorStyle(Colours.playerName)
                                .withBold(true)
                        )
                );

        source.sendSuccess(() -> successMessage, false);
        targetPlayer.sendSystemMessage(successMessage);

        LOGGER.info("[{}] {} added {}'s currency by {}{}",
                LOGNAME,
                sourcePlayer.getName().getString(),
                targetData.name,
                Config.currencySymbol,
                amount
        );

        return 1;
    }
}
