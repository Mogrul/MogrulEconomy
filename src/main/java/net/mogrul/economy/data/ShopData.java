package net.mogrul.economy.data;

public class ShopData {
    public String shopUUID;
    public String entityName;
    public String entityLevel;
    public String sellingItemID;
    public int sellingAmount;
    public int sellingPrice;
    public double entityX;
    public double entityZ;
    public int entityY;
    public float entityYaw;
    public float entityPitch;
    public int quantitySold;

    public ShopData(
            String shopUUID,
            String EntityName,
            String entityLevel,
            String sellingItemID,
            int sellingAmount,
            int sellingPrice,
            double entityX,
            double entityZ,
            int entityY,
            float entityYaw,
            float entityPitch,
            int quantitySold
    ) {
        this.shopUUID = shopUUID;
        this.entityName = EntityName;
        this.entityLevel = entityLevel;
        this.sellingItemID = sellingItemID;
        this.sellingAmount = sellingAmount;
        this.sellingPrice = sellingPrice;
        this.entityX = entityX;
        this.entityY = entityY;
        this.entityZ = entityZ;
        this.entityYaw = entityYaw;
        this.entityPitch = entityPitch;
        this.quantitySold = quantitySold;
    }
}
