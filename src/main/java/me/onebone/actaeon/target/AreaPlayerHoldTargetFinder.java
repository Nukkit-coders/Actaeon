package me.onebone.actaeon.target;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import me.onebone.actaeon.entity.MovingEntity;

public class AreaPlayerHoldTargetFinder extends TargetFinder {

    private Item[] items;
    private int radius;

    public AreaPlayerHoldTargetFinder(MovingEntity entity, long interval, Item[] items, int radius) {
        super(entity, interval);
        this.items = items;
        this.radius = radius;
    }

    protected void find() {
        Player near = null;
        double nearest = this.radius * this.radius;

        for (Player player : this.getEntity().getLevel().getPlayers().values()) {
            if (this.getEntity().distanceSquared(player) < nearest) {
                for (Item item : this.items) {
                    if (player.getInventory().getItemInHand().equals(item, false, false)) {
                        near = player;
                        nearest = this.getEntity().distance(player);
                        break; // Jump out of the single-layer loop
                    }
                }
            }
        }

        if (near != null) {
            this.getEntity().setTarget(near, this.getEntity().getName());
            this.getEntity().setHate(near);
        } else {
            //this.getEntity().getRoute().forceStop();
            this.getEntity().setTarget(null, this.getEntity().getName());
        }
    }
}
