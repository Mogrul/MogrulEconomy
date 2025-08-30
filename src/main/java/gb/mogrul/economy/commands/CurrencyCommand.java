package gb.mogrul.economy.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import static gb.mogrul.economy.MogrulEconomy.*;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import gb.mogrul.economy.Config;
import gb.mogrul.economy.Memory;
import gb.mogrul.economy.player.PlayerData;
import gb.mogrul.economy.player.PlayerSQLHandler;

@EventBusSubscriber(modid = MODID)
public class CurrencyCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LOGGER.info("[{}] Registering currency commands!", LOGNAME);
        dispatcher.register(
            Commands.literal(Config.economyCommandName)
                .executes(context -> getCurrency(
                    context.getSource()
                ))

                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> getOthersCurrency(
                        context.getSource(),
                        EntityArgument.getPlayer(context, "target")
                    ))
                )

                .then(Commands.literal("send")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(context -> sendCurrency(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "amount"),
                                EntityArgument.getPlayer(context, "target")
                            ))
                        )
                    )
                )

                .then(Commands.literal("remove")
                    .requires(source -> source.hasPermission(4))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(context -> removeCurrency(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "amount"),
                                EntityArgument.getPlayer(context, "target")
                            ))
                        )
                    )
                )

                .then(Commands.literal("set")
                    .requires(source -> source.hasPermission(4))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(context -> setCurrency(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "amount"),
                                EntityArgument.getPlayer(context, "target")
                            ))
                        )
                    )
                )

                .then(Commands.literal("add")
                    .requires(source -> source.hasPermission(4))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(context -> addCurrency(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "amount"),
                                EntityArgument.getPlayer(context, "target")
                            ))
                        )
                    )
                )
        );
    }

    private static int getCurrency(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("Only players can issue this command!"));
            return 0;
        };

        PlayerData playerData = Memory.players.get(sourcePlayer.getStringUUID());

        Component successMessage = Component.literal("You have: ")
            .append(Component.literal(Config.economyCurrencySymbol + playerData.currency));

        source.sendSuccess(() -> successMessage, false);

        LOGGER.info("[{}] {} requested to view their {}",
            LOGNAME,
            sourcePlayer.getScoreboardName(),
            Config.economyCurrencyNamePlural
        );
        
        return 1;
    }

    private static int getOthersCurrency(CommandSourceStack source, ServerPlayer targetPlayer) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("Only players can issue this command!"));
            return 0;
        };

        PlayerData playerData = Memory.players.get(targetPlayer.getStringUUID());

        Component successMessage = Component.literal(targetPlayer.getScoreboardName())
            .append(Component.literal(" has "))
            .append(Component.literal(Config.economyCurrencySymbol + playerData.currency));
        
        source.sendSuccess(() -> successMessage, false);
        
        LOGGER.info("[{}] {} requested to view {}'s {}",
            LOGNAME,
            sourcePlayer.getScoreboardName(),
            targetPlayer.getScoreboardName()
        );

        return 1;
    }

    private static int sendCurrency(CommandSourceStack source, Integer amount, ServerPlayer targetPlayer) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("Only players can issue this command!"));
            return 0;
        };

        if (!sourcePlayer.getScoreboardName().equals("Dev") 
            && sourcePlayer.getUUID().equals(targetPlayer.getUUID())
        ) {
            source.sendFailure(Component.literal("You can't send " + Config.economyCurrencyNamePlural + " to yourself!"));
        }

        PlayerData sourcePlayerData = Memory.players.get(sourcePlayer.getStringUUID());
        if (sourcePlayerData.currency < amount) {
            source.sendFailure(Component.literal("You don't have enough " + Config.economyCurrencyNamePlural + " to send!"));
            return 0;
        }

        PlayerData targetPlayerData = Memory.players.get(targetPlayer.getStringUUID());
        sourcePlayerData.currency -= amount;
        targetPlayerData.currency += amount;

        PlayerSQLHandler.update(sourcePlayerData);
        PlayerSQLHandler.update(targetPlayerData);

        Component sourceSuccessMessage = Component.literal("You sent ")
            .append(Component.literal(Config.economyCurrencySymbol + amount))
            .append(Component.literal(" to "))
            .append(Component.literal(targetPlayerData.username));
        
        Component targetSuccessMessage = Component.literal("You've received ")
            .append(Component.literal(Config.economyCurrencySymbol + amount))
            .append(Component.literal(" from "))
            .append(Component.literal(sourcePlayerData.username));
        
        source.sendSuccess(() -> sourceSuccessMessage, false);
        targetPlayer.sendSystemMessage(targetSuccessMessage);

        LOGGER.info("[{}] {} sent {} to {}",
            LOGNAME,
            sourcePlayer.getScoreboardName(),
            Config.economyCurrencySymbol + amount,
            targetPlayer.getScoreboardName()
        );

        return 1;
    }

    private static int removeCurrency(CommandSourceStack source, Integer amount, ServerPlayer targetPlayer) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("Only players can issue this command!"));
            return 0;
        };

        PlayerData targetPlayerData = Memory.players.get(targetPlayer.getStringUUID());
        targetPlayerData.currency = Math.max(0, targetPlayerData.currency - amount);
        PlayerSQLHandler.update(targetPlayerData);

        Component sourceSuccessMessage = Component.literal("Removed ")
            .append(Component.literal(Config.economyCurrencySymbol + amount))
            .append(Component.literal(" from "))
            .append(Component.literal(targetPlayerData.username));
        
        Component targetSuccessMessage = Component.literal(sourcePlayer.getScoreboardName())
            .append(Component.literal(" removed "))
            .append(Component.literal(Config.economyCurrencySymbol + amount))
            .append(Component.literal(" from you!"));

        source.sendSuccess(() -> sourceSuccessMessage, false);
        targetPlayer.sendSystemMessage(targetSuccessMessage);

        LOGGER.info("[{}] {} removed {} from {}",
            LOGNAME,
            sourcePlayer.getScoreboardName(),
            Config.economyCurrencySymbol + amount,
            targetPlayer.getScoreboardName()
        );

        return 1;
    }

    private static int setCurrency(CommandSourceStack source, Integer amount, ServerPlayer targetPlayer) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("Only players can issue this command!"));
            return 0;
        };

        PlayerData targetPlayerData = Memory.players.get(targetPlayer.getStringUUID());
        targetPlayerData.currency = amount;
        PlayerSQLHandler.update(targetPlayerData);

        Component sourceSuccessMessage = Component.literal("Set ")
            .append(Component.literal(targetPlayer.getScoreboardName()))
            .append(Component.literal("'s "))
            .append(Component.literal(Config.economyCurrencyNamePlural))
            .append(Component.literal(" to "))
            .append(Component.literal(Config.economyCurrencySymbol + amount));
        
        Component targetSuccessMessage = Component.literal(sourcePlayer.getScoreboardName())
            .append(Component.literal(" set your "))
            .append(Component.literal(Config.economyCurrencyNamePlural))
            .append(Component.literal(" to "))
            .append(Component.literal(Config.economyCurrencySymbol + amount));
        
        source.sendSuccess(() -> sourceSuccessMessage, false);
        targetPlayer.sendSystemMessage(targetSuccessMessage);

        LOGGER.info("[{}] {} set {}'s {} to {}!",
            LOGNAME,
            sourcePlayer.getScoreboardName(),
            targetPlayer.getScoreboardName(),
            Config.economyCurrencyNamePlural,
            Config.economyCurrencySymbol + amount
        );

        return 1;
    }

    private static int addCurrency(CommandSourceStack source, Integer amount, ServerPlayer targetPlayer) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("Only players can issue this command!"));
            return 0;
        };

        PlayerData targetPlayerData = Memory.players.get(targetPlayer.getStringUUID());
        targetPlayerData.currency += amount;
        PlayerSQLHandler.update(targetPlayerData);

        Component sourceSuccessMessage = Component.literal("Added ")
            .append(Component.literal(Config.economyCurrencySymbol + amount))
            .append(Component.literal(" to "))
            .append(Component.literal(targetPlayerData.username));
        
        Component targetSuccessMessage = Component.literal(sourcePlayer.getScoreboardName())
            .append(Component.literal(" added "))
            .append(Component.literal(Config.economyCurrencySymbol + amount))
            .append(Component.literal(" to you!"));
        
        source.sendSuccess(() -> sourceSuccessMessage, false);
        targetPlayer.sendSystemMessage(targetSuccessMessage);

        return 1;
    }
}
