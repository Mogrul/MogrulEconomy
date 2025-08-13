package net.mogrul.economy.subs;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.mogrul.economy.Config;
import net.mogrul.economy.data.MemoryData;
import net.mogrul.economy.data.PlayerData;
import net.mogrul.economy.data.ShopData;
import net.mogrul.economy.handlers.PlayerDataHandler;
import net.mogrul.economy.handlers.ShopDataHandler;
import org.jetbrains.annotations.NotNull;

import static net.mogrul.economy.MogrulEconomy.*;

public class ShopVillager extends Villager {
    public ShopVillager(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    @Override
    protected void customServerAiStep() {
        // Disable movement.
    }

    @Override
    public void push(@NotNull Entity entity) {
        // Disable pushing.
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // Disable damage.
        return false;
    }

    @Override
    public void die(@NotNull DamageSource cause) {
        // Disable death.
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            // Make it jump!
            if (onGround()) {
                double jumpDistance = 0.2;
                setDeltaMovement(getDeltaMovement().x, jumpDistance, getDeltaMovement().z);
            }

            // Handle client buying.
            ShopData shopData = MemoryData.shops.get(getStringUUID());
            PlayerData playerData = MemoryData.players.get(serverPlayer.getStringUUID());

            if (shopData == null || playerData == null) {
                LOGGER.error("[{}] Failed to sell from [{}] to {}: shopData or playerData is null.",
                        LOGNAME,
                        getStringUUID(),
                        serverPlayer.getName().getString()
                );
                return InteractionResult.FAIL;
            }

            // Not enough money!
            if (shopData.sellingPrice > playerData.currency) {
                Component notEnoughMoneyFailureMessage = Component.literal("[Shops]\n")
                        .append(Component.literal("Not enough money!\n"))
                        .append(Component.literal(shopData.sellingItemID)
                                .withStyle(ChatFormatting.GREEN)
                        )
                        .append(Component.literal(" x"))
                        .append(Component.literal(shopData.sellingAmount + " ")
                                .withStyle(ChatFormatting.GREEN)
                        )
                        .append(Component.literal(Config.currencySymbol + shopData.sellingPrice + "\n")
                                .withStyle(ChatFormatting.BLUE)
                        )
                        .append(Component.literal(playerData.name + " ")
                                .withStyle(ChatFormatting.GREEN)
                        )
                        .append(Component.literal(Config.currencySymbol + playerData.currency)
                                .withStyle(ChatFormatting.RED)
                        );

                serverPlayer.sendSystemMessage(notEnoughMoneyFailureMessage);
                return InteractionResult.FAIL;
            }

            // Get the items.
            ResourceLocation resourceLocation = ResourceLocation.tryParse(shopData.sellingItemID);
            if (resourceLocation == null) {
                LOGGER.error("[{}] Failed to get ResourceLocation from ID {}", LOGNAME, shopData.sellingItemID);
                return InteractionResult.FAIL;
            }

            Item sellingItem = BuiltInRegistries.ITEM.get(resourceLocation);
            ItemStack sellingItemStack = new ItemStack(sellingItem, shopData.sellingAmount).copy();

            if (!canFitInPlayerInventory(serverPlayer.getInventory(), sellingItemStack)) {
                serverPlayer.sendSystemMessage(Component.literal("Make some room in your inventory!"));
                return InteractionResult.FAIL;
            }

            serverPlayer.getInventory().add(sellingItemStack);

            // Remove currency from player and give items.
            playerData.currency -= shopData.sellingPrice;
            PlayerDataHandler.save(playerData);
            shopData.quantitySold += shopData.sellingAmount;
            ShopDataHandler.save(shopData);

            Component successfulTransactrionMessage = Component.literal("[Shops]\n")
                    .append(Component.literal("You've bought "))
                    .append(Component.literal(shopData.sellingItemID + " x" + shopData.sellingAmount + "\n")
                            .withStyle(ChatFormatting.GREEN)
                    )
                    .append(Component.literal(playerData.name)
                            .withStyle(ChatFormatting.BLUE)
                    )
                    .append(Component.literal(Config.currencySymbol + shopData.sellingPrice )
                            .withStyle(ChatFormatting.YELLOW)
                    )
                    .append(Component.literal(" -> "))
                    .append(Component.literal(shopData.entityName + "\n")
                            .withStyle(ChatFormatting.BLUE)
                    )
                    .append(Component.literal(playerData.name)
                            .withStyle(ChatFormatting.GREEN)
                    )
                    .append(Component.literal(" = "))
                    .append(Component.literal(Config.currencySymbol + playerData.currency)
                            .withStyle(ChatFormatting.YELLOW)
                    );

            serverPlayer.sendSystemMessage(successfulTransactrionMessage);
        }

        return InteractionResult.FAIL;
    }
}
