package net.mogrul.economy.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.mogrul.economy.data.ShopData;
import net.mogrul.economy.handlers.ShopDataHandler;

import java.util.EnumSet;

public class ShopVillagerGoal extends Goal {
    private final Villager villager;
    private final ShopData shopData;

    // Applied goals
    private final LookAtPlayerGoal lookAtPlayerGoal;

    public ShopVillagerGoal(Villager villager) {
        this.villager = villager;
        this.shopData = getShopData(villager.getStringUUID());
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.lookAtPlayerGoal = new LookAtPlayerGoal(villager, Player.class, 8.0F);

        applyAttributes();
        setPosition();
    }

    private void applyAttributes() {
        villager.setInvulnerable(true);
        villager.hasImpulse = false;
    }

    private void setPosition() {
        villager.setPos(shopData.entityX, shopData.entityY, shopData.entityZ);
    }

    private ShopData getShopData(String villagerUUID) {
        return ShopDataHandler.load(villagerUUID);
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {

        villager.setDeltaMovement(0, villager.getDeltaMovement().y, 0);
        villager.setPos(shopData.entityX, villager.getY(), shopData.entityZ);

        villager.getNavigation().stop();
        //setPosition();

        if (lookAtPlayerGoal.canUse()) {
            lookAtPlayerGoal.tick();
        }
    }
}
