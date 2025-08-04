package com.mogrul.economy.commands;

import com.mogrul.economy.MogrulEconomy;
import com.mogrul.economy.utils.Config;
import com.mogrul.economy.utils.database.CurrencyManager;
import com.mogrul.economy.utils.database.TradeManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.*;
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
                Commands.literal(Config.tradeCommandName)
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

        dispatcher.register(
                Commands.literal("canceltrade")
                        .then(Commands.argument("tradeId", StringArgumentType.word())
                                .executes(context -> cancelTrade(
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
            source.sendFailure(Component.literal("You're not holding " + String.format("%,d", amount) + " items!"));
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

        // [TRADE REQUEST]
        // From: {name}
        // Items: {count} {itemName}
        // Price: {symbol} {price}
        // [ACCEPT] [REJECT]
        Component receiverMessage = Component.literal("[TRADE REQUEST]\n")
                .withStyle(Style.EMPTY.withBold(true).withColor(TextColor.parseColor("#C6F601")))
                .append(Component.literal("From: ")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false)))
                .append(Component.literal(sender.getGameProfile().getName())
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FB7100")).withBold(false)))
                .append(Component.literal("\nItems: ")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false)))
                .append(Component.literal(String.format("%,d", amount) + " ")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#A8FF01")).withBold(false)))
                .append(stackCopy.getHoverName().copy()
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#A8FF01")).withBold(false)))
                .append(Component.literal("\nPrice: ")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false)))
                .append(Component.literal(Config.currencySymbol + String.format("%,d", price))
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFDA00")).withBold(false)))
                .append(Component.literal("\n[ACCEPT] ")
                        .withStyle(style -> style
                                .withColor(TextColor.parseColor("#00FE05"))
                                .withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accepttrade " + tradeId))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Click to accept this trade.")))))
                .append(Component.literal(" [REJECT]")
                        .withStyle(style -> style
                                .withColor(TextColor.parseColor("#FE0301"))
                                .withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rejecttrade " + tradeId))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Click to reject this trade.")))));

        // [TRADE REQUEST SENT]
        // To: {name}
        // Items: {count} {itemName}
        // Price: {symbol}{cost}
        // [CANCEL]
        Component senderMessage = Component.literal("[TRADE REQUEST SENT]\n")
                .withStyle(Style.EMPTY.withBold(true).withColor(TextColor.parseColor("#C6F601")))
                .append(Component.literal("To: ")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false)))
                .append(Component.literal(target.getDisplayName().getString())
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FB7100")).withBold(false)))
                .append(Component.literal("\nItems: ")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false)))
                .append(Component.literal(String.format("%,d", amount) + " ")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#A8FF01")).withBold(false)))
                .append(stackCopy.getHoverName().copy()
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#A8FF01")).withBold(false)))
                .append(Component.literal("\nPrice: ")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false)))
                .append(Component.literal(Config.currencySymbol + String.format("%,d", price))
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFDA00")).withBold(false)))
                .append(Component.literal("\n[CANCEL]")
                        .withStyle(style -> style
                                .withColor(TextColor.parseColor("#FE0301"))
                                .withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/canceltrade " + tradeId))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Click to cancel this trade."))
                                ))
                );


        LOGGER.info(
                "[{}] Trade of {} {} for {}{} from {} sent to {}",
                MogrulEconomy.MODID,
                String.format("%,d", amount),
                stackCopy.getHoverName().getString(),
                Config.currencySymbol,
                String.format("%,d", price),
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

        Component accepterMessage = Component.literal("Accepted trade of ")
                .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false))
                .append(Component.literal(String.format("%,d", trade.count()) + " " + trade.item().getHoverName().getString())
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#A8FF01")).withBold(true)))
                .append(Component.literal(" from ")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false)))
                .append(Component.literal(trade.fromPlayer().getDisplayName().getString())
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FB7100")).withBold(true)))
                .append(Component.literal(" for ")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false)))
                .append(Component.literal(Config.currencySymbol + String.format("%,d", trade.price()))
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFDA00")).withBold(false)));

        Component sellerMessage = Component.literal(trade.fromPlayer().getDisplayName().getString())
                .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FB7100")).withBold(true))
                .append(Component.literal(" accepted your trade of ")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false)))
                .append(Component.literal(String.format("%,d", trade.count()) + " " + trade.item().getHoverName().getString())
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#A8FF01")).withBold(true)))
                .append(Component.literal(" for ")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false)))
                .append(Component.literal(Config.currencySymbol + String.format("%,d", trade.price()))
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFDA00")).withBold(false)));


        source.sendSuccess(() -> accepterMessage, true);
        trade.fromPlayer().sendSystemMessage(sellerMessage);

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

    public static int cancelTrade(CommandSourceStack source, String tradeId) {
        ServerPlayer toPlayer = source.getPlayer();
        PendingTrade trade = pendingTrades.remove(tradeId);

        if (checkTradeAuth(source, toPlayer, trade)) return 0;

        assert trade != null;
        if (System.currentTimeMillis() > trade.expiresAt()) {
            source.sendFailure(Component.literal("Trade expired!"));
            return 0;
        }

        Component sourceMessage = Component.literal("Trade request to ")
                .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false))
                .append(Component.literal(trade.toPlayer().getGameProfile().getName())
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FB7100")).withBold(false)))
                .append(Component.literal(" cancelled")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FE0301")).withBold(true)));

        Component receiverMessage = Component.literal(trade.fromPlayer().getDisplayName().getString())
                .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false))  // Non-bold white for player name
                .append(Component.literal(" cancelled the trade.")
                        .withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")).withBold(false)));  // Non-bold white for "called the trade"

        source.sendSuccess(() -> sourceMessage, true);
        trade.toPlayer().sendSystemMessage(receiverMessage);

        sendItemsBack(trade.fromPlayer(), trade.item());

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
