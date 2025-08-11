package net.mogrul.economy.data;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class PendingTradeData {
    public static String UUID;
    public static ServerPlayer fromPlayer;
    public static ServerPlayer toPlayer;
    public static ItemStack items;
    public static int count;
    public static int price;
    public static long expiresAt;

    public PendingTradeData(
            String id,
            ServerPlayer fromPlayer,
            ServerPlayer toPlayer,
            ItemStack items,
            int count,
            int price,
            long expiresAt)
    {
        this.UUID = UUID;
        this.fromPlayer = fromPlayer;
        this.toPlayer = toPlayer;
        this.items = items;
        this.count = count;
        this.price = price;
        this.expiresAt = expiresAt;

    }
}
