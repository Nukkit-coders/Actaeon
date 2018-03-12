package me.onebone.actaeon.hook;

import cn.nukkit.Server;
import me.onebone.actaeon.entity.MovingEntity;

/**
 * Mob
 *
 * @author WetABQ Copyright (c) 2018.03
 * @version 1.0
 */
public class SheepWoolHook extends MovingEntityHook {
    private long woolTick;

    public SheepWoolHook(MovingEntity animal, int woolTick) {
        super(animal);
        this.woolTick = Server.getInstance().getTick() + woolTick;
    }

    @Override
    public void onUpdate(int tick) {
        if (tick >= woolTick) {
            if (entity.namedTag.contains("Sheared") && entity.namedTag.getByte("Sheared") != 0) {
                entity.namedTag.putByte("Sheared",1);
            }
        }
    }

}
