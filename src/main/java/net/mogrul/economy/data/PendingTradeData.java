package net.mogrul.economy.data;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class PendingTradeData {
    public String UUID;
    public ServerPlayer fromPlayer;
    public ServerPlayer toPlayer;
    public ItemStack items;
    public int count;
    public int price;
    public long expiresAt;

    public PendingTradeData(
            String UUID,
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
