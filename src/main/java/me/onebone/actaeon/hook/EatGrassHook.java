package me.onebone.actaeon.hook;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
//import cn.nukkit.block.BlockDirt;
import cn.nukkit.block.BlockGrass;
import cn.nukkit.block.BlockTallGrass;
//import cn.nukkit.level.particle.DestroyBlockParticle;
import cn.nukkit.network.protocol.EntityEventPacket;
import me.onebone.actaeon.Utils.Utils;
import me.onebone.actaeon.entity.animal.Animal;

/**
 * Created by CreeperFace on 15.7.2017.
 */
public class EatGrassHook extends MovingEntityHook {

    private long nextEatGrass = 0;
    private EntityEventPacket packet = new EntityEventPacket();

    public EatGrassHook(Animal entity){
        super(entity);
        packet.eid = entity.getId();
        packet.event = EntityEventPacket.EAT_GRASS_ANIMATION;
        next();
    }

    @Override
    public void onUpdate(int tick) {
        if (Server.getInstance().getTick() >= this.nextEatGrass && packet != null && this.nextEatGrass != 0) {
            Block block = this.entity.getLevelBlock();
            if (block instanceof BlockTallGrass || block.down() instanceof BlockGrass) {
                Server.broadcastPacket(entity.getLevel().getPlayers().values(), packet);
                next();
            }
        }
    }

    private void next(){
        this.nextEatGrass = (((Animal)this.entity).isBaby()) ?
                Server.getInstance().getTick() + Utils.rand(20 * 60, 20 * 180) :
                Server.getInstance().getTick() + Utils.rand(20 * 40, 20 * 120);
    }
}