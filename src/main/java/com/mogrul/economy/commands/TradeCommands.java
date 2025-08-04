package com.mogrul.economy.commands;

import com.mogrul.economy.MogrulEconomy;
import com.mogrul.economy.utils.Config;
import com.mogrul.economy.utils.database.CurrencyManager;
import com.mogrul.economy.utils.database.TradeManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Map<String, PendingTrade> pendingTrades = new HashMap<>();

    public record PendingTrade(
            String id,
            ServerPlayer fromPlayer,
            ServerPlayer toPlayer,
            ItemStack item,
            int count,
            int price,
            long expiresAt
    ) {}

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("[{}] Registering trade commands...", MogrulEconomy.MODID);

        dispatcher.register(
                Commands.literal("trade")
                        .requires(source -> source.hasPermission(0))

                        // Member command: /trade <amount> <player> - Sends a trade request from the item in their hand to a player.
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("price", IntegerArgumentType.integer(0))
                                                .executes(context -> sendTradeRequest(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "amount"),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "price")
                                                ))
                                        )
                                )
                        )
        );

        dispatcher.register(
                Commands.literal("accepttrade")
                        .then(Commands.argument("tradeId", StringArgumentType.word())
                                .executes(context -> acceptTrade(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "tradeId")
                                ))
                        )
        );

        dispatcher.register(
                Commands.literal("rejecttrade")
                        .then(Commands.argument("tradeId", StringArgumentType.word())
                                .executes(context -> rejectTrade(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "tradeId")
                                ))
                        )
        );
    }

    public static int sendTradeRequest(CommandSourceStack source, Integer amount, ServerPlayer target, Integer price) {
        ServerPlayer sender = source.getPlayer();

        assert sender != null;
        ItemStack heldStack = sender.getMainHandItem();

        if (!sender.getDisplayName().getString().equals("Dev")) {
            if (sender.getStringUUID().equals(target.getStringUUID())) {
                source.sendFailure(Component.literal("You can't trade with yourself!"));
                return 0;
            }
        }

        if (heldStack.isEmpty()) {
            source.sendFailure(Component.literal("You're not holding any item!"));
            return 0;
        }

        if (heldStack.getCount() < amount) {
            source.sendFailure(Component.literal("You're not holding " + amount + " items!"));
            return 0;
        }

        if (source.getServer().getPlayerList().getPlayerByName(target.getGameProfile().getName()) == null) {
            source.sendFailure(Component.literal("Player " + target.getGameProfile().getName() + " not found!"));
            return 0;
        }

        ItemStack stackCopy = heldStack.copy();
        stackCopy.setCount(amount);
        sender.getMainHandItem().shrink(amount);

        String tradeId = UUID.randomUUID().toString();
        long expiresAt = System.currentTimeMillis() + 30_000L;

        PendingTrade trade = new PendingTrade(tradeId, sender, target, stackCopy, amount, price, expiresAt);
        pendingTrades.put(tradeId, trade);

        TradeManager.addTrade(trade);

        Component receiverMessage = Component.literal(sender.getGameProfile().getName() + " wants to trade you ")
                .append(Component.literal(amount + " ").withStyle(ChatFormatting.GOLD))
                .append(stackCopy.getHoverName().copy().withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" for "))
                .append(Component.literal(Config.currencySymbol + price).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" (expires in 30s). ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("[Accept]").withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accepttrade " + tradeId))
                        .withHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Click to accept this trade."))
                        ))
                )
                .append(Component.literal(" [Reject]").withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rejecttrade " + tradeId))
                        .withHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Click to reject this trade."))
                        ))
                );

        Component senderMessage = Component.literal("Trade request of ")
                .append(Component.literal(amount + " ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(stackCopy.getHoverName().getString()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" sent to " + target.getDisplayName().getString()))
                .append(Component.literal(" for "))
                .append(Component.literal(Config.currencySymbol + price).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" (expires in 30s). ").withStyle(ChatFormatting.YELLOW));

        LOGGER.info(
                "[{}] Trade of {} {} for {}{} from {} sent to {}",
                MogrulEconomy.MODID,
                amount,
                stackCopy.getHoverName().getString(),
                Config.currencySymbol,
                price,
                sender.getGameProfile().getName(),
                target.getGameProfile().getName()
        );

        target.sendSystemMessage(receiverMessage);
        sender.sendSystemMessage(senderMessage);

        return 1;
    }

    public static int acceptTrade(CommandSourceStack source, String tradeId) {
        ServerPlayer toPlayer = source.getPlayer();

        PendingTrade trade = pendingTrades.get(tradeId);
        if (checkTradeAuth(source, toPlayer, trade)) return 0;

        assert trade != null;
        if (System.currentTimeMillis() > trade.expiresAt()) {
            source.sendFailure(Component.literal("Trade expired!"));
            return 0;
        }

        int toPlayerCurrency = CurrencyManager.getCurrency(toPlayer);
        if (toPlayerCurrency < trade.price()) {
            source.sendFailure(Component.literal("You don't have enough money!"));
            trade.fromPlayer().sendSystemMessage(
                    Component.literal(source.getTextName() + " doesn't have enough money for the trade!")
            );

            sendItemsBack(trade.fromPlayer(), trade.item());

            return 0;
        }

        if (!toPlayer.getInventory().add(trade.item().copy())) {
            toPlayer.drop(trade.item().copy(), false);
        }

        CurrencyManager.removeCurrency(toPlayer, trade.price());
        CurrencyManager.addCurrency(trade.fromPlayer(), trade.price());

        LOGGER.info(
                "[{}] Trade from {} to {} accepted.",
                MogrulEconomy.MODID,
                toPlayer.getDisplayName().getString(),
                trade.toPlayer().getDisplayName().getString()
        );

        source.sendSuccess(() -> Component.literal(
                "You accepted the trade and received " +
                        trade.count() + " " +
                        trade.item().getHoverName().getString()),
                true
        );
        trade.fromPlayer().sendSystemMessage(Component.literal(toPlayer.getName().getString() + " accepted your trade."));

        TradeManager.updateTradeAccepted(trade);
        pendingTrades.remove(tradeId);

        return 1;
    }

    public static int rejectTrade(CommandSourceStack source, String tradeId) {
        ServerPlayer toPlayer = source.getPlayer();

        PendingTrade trade = pendingTrades.remove(tradeId);
        if (checkTradeAuth(source, toPlayer, trade)) return 0;

        assert toPlayer != null;
        LOGGER.info("[{}] Trade from {} to {} rejected.", MogrulEconomy.MODID, toPlayer.getName(), trade.toPlayer().getName());

        source.sendSuccess(() -> Component.literal(
                "You rejected the trade from " +
                        trade.fromPlayer().getName().getString()),
                true
        );
        trade.fromPlayer().sendSystemMessage(Component.literal(toPlayer.getName().getString() + " rejected your trade."));
        sendItemsBack(trade.fromPlayer(), trade.item());

        pendingTrades.remove(tradeId);

        return 1;
    }

    private static boolean checkTradeAuth(CommandSourceStack source, ServerPlayer toPlayer, PendingTrade trade) {
        if (trade == null) {
            source.sendFailure(Component.literal("Trade no longer exists!"));
            return true;
        }

        assert toPlayer != null;
        if (!trade.toPlayer().getUUID().equals(toPlayer.getUUID())) {
            source.sendFailure(Component.literal("This trade was not sent to you!"));
            return true;
        }
        return false;
    }

    public static void onTradeExpire(PendingTrade trade) {
        LOGGER.info(
                "[{}] Trade from {} to {} expired.",
                MogrulEconomy.MODID,
                trade.fromPlayer().getName(),
                trade.toPlayer().getName()
        );

        trade.fromPlayer().sendSystemMessage(Component.literal(
                "Your trade to " +
                        trade.toPlayer().getDisplayName().getString() +
                        " expired.")
        );
        trade.toPlayer().sendSystemMessage(Component.literal(
                "Trade offer from " +
                        trade.fromPlayer().getDisplayName().getString() +
                        " expired.")
        );

        sendItemsBack(trade.fromPlayer(), trade.item());
    }

    private static void sendItemsBack(ServerPlayer fromPlayer, ItemStack item) {
        if (!fromPlayer.getInventory().add(item.copy())) {
            fromPlayer.drop(item.copy(), false);
        }
    }
}
