package me.onebone.actaeon.entity.animal;

import cn.nukkit.entity.EntityAgeable;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import me.onebone.actaeon.Utils.Utils;
import me.onebone.actaeon.hook.AnimalGrowHook;
import me.onebone.actaeon.hook.AnimalHook;

import java.util.Random;

public class Pig extends Animal implements EntityAgeable{
	public static final int NETWORK_ID = 12;
	private boolean isBaby = false;

	public Pig(FullChunk chunk, CompoundTag nbt) {
		super(chunk, nbt);
		isBaby = Utils.rand(1,3) == 1;
		setBaby(isBaby);
		if(isBaby){
			this.addHook("grow", new AnimalGrowHook(this, Utils.rand(20*60*10,20*60*20)));
		}
		this.addHook("targetFinder", new AnimalHook(this, 500, new Item[]{
				Item.get(Item.CARROTS),
				Item.get(Item.CARROT_ON_A_STICK),
				Item.get(Item.POTATO),
				Item.get(Item.BEETROOT)},
				5)); //Pigs are attracted to players who hold carrots, carrot rods, potatoes, or beets
	}

	@Override
	public float getWidth() {
		if (this.isBaby()) {
			return 0.45f;
		}
		return 0.9f;
	}

	@Override
	public float getHeight() {
		if (this.isBaby()) {
			return 0.45f;
		}
		return 0.9f;
	}

	@Override
	public float getEyeHeight() {
		if (this.isBaby()) {
			return 0.45f;
		}
		return 0.9f;
	}

	@Override
	public boolean entityBaseTick(int tickDiff){
		return super.entityBaseTick(tickDiff);
	}

	@Override
	public String getName() {
		return this.getNameTag();
	}

	@Override
	public Item[] getDrops() {
		if(!isBaby()) {
			Random random = new Random();
			this.getLevel().dropExpOrb(this,random.nextInt(3) + 1);
			int count = random.nextInt(3) + 1;
			Item meat = Item.get(Item.RAW_PORKCHOP, 0, count);
			EntityDamageEvent cause = this.getLastDamageCause();
			if (cause.getCause() == EntityDamageEvent.DamageCause.FIRE) {
				meat = Item.get(Item.COOKED_PORKCHOP, 0, count);
			}
			return new Item[]{meat};
		}else{
			return new Item[0];
		}
	}

	@Override
	public int getNetworkId() {
		return NETWORK_ID;
	}

	@Override
	protected void initEntity() {
		super.initEntity();
		setMaxHealth(10);
	}

	@Override
	public boolean isBaby(){
		return isBaby;
	}
}
