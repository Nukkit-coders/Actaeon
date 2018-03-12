package me.onebone.actaeon.entity.animal;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.data.ByteEntityData;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemDye;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.utils.DyeColor;
import me.onebone.actaeon.Utils.Utils;
import me.onebone.actaeon.hook.AnimalGrowHook;
import me.onebone.actaeon.hook.AnimalHook;
import me.onebone.actaeon.hook.SheepWoolHook;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Sheep extends Animal {
	public static final int NETWORK_ID = 13;

	private boolean sheared = false;
	private int color = 0;
	private EntityEventPacket packet;
	private long nextEatGlass = 0;

	public Sheep(FullChunk chunk, CompoundTag nbt) {
		super(chunk, nbt);
		boolean isBaby = Utils.rand(1, 3) == 1;
		setBaby(isBaby);
		if (isBaby) {
			this.addHook("grow", new AnimalGrowHook(this, Utils.rand(20 * 60 * 10, 20 * 60 * 20)));
		}
		this.addHook("targetFinder", new AnimalHook(this, 500, new Item[]{Item.get(Item.WHEAT)}, 10));
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
		if (isBaby()) {
			return 0.65f;
		}
		return 1.3f;
	}

	@Override
	public float getEyeHeight() {
		if (isBaby()) {
			return 0.65f;
		}
		return 1.1f;
	}

	@Override
	public String getName() {
		return this.getNameTag();
	}

	@Override
	public Item[] getDrops() {
		if (!isBaby()) {
			Random random = new Random();
			int meatCount = random.nextInt(3) + 1;
			Item meat = Item.get(Item.RAW_MUTTON, 0, meatCount);
			Item wool = Item.get(Item.WOOL, color, (sheared) ? 1 : 0);
			EntityDamageEvent cause = this.getLastDamageCause();
			if (cause.getCause() == EntityDamageEvent.DamageCause.FIRE) {
				meat = Item.get(Item.COOKED_MUTTON, 0, meatCount);
			}
			this.getLevel().dropExpOrb(this, random.nextInt(3) + 1);
			return new Item[]{wool, meat};
		} else {
			return new Item[0];
		}
	}

	@Override
	public boolean onInteract(Player player, Item item) {
		if (item.getId() == Item.DYE) {
			this.setColor(((ItemDye) item).getDyeColor().getWoolData());
			return true;
		}

		return item.getId() == Item.SHEARS && shear();
	}

	@Override
	public int getNetworkId() {
		return NETWORK_ID;
	}

	@Override
	public boolean entityBaseTick(int tickDiff) {
		if (Server.getInstance().getTick() >= this.nextEatGlass && packet != null && this.nextEatGlass != 0) {
			Server.broadcastPacket(this.getLevel().getPlayers().values(), packet);
			this.nextEatGlass = Server.getInstance().getTick() + Utils.rand(20*60,20*120);
		}
		return super.entityBaseTick(tickDiff);
	}

	@Override
	protected void initEntity() {
		super.initEntity();
		this.setMaxHealth(8);
		if (namedTag.contains("Color")) {
			int color = new Random().nextInt(15);
			this.setDataProperty(new ByteEntityData(DATA_COLOUR, color));
			this.namedTag.putByte("Color", this.color);
		}
		if (namedTag.contains("Sheared")) {
			namedTag.putByte("Sheared", 0); //Wool is trimmed and the data value becomes 1
		}
		if (!this.namedTag.contains("Color")) {
			this.setColor(randomColor());
		} else {
			this.setColor(this.namedTag.getByte("Color"));
		}

		if (!this.namedTag.contains("Sheared")) {
			this.namedTag.putByte("Sheared", 0);
		} else {
			this.sheared = this.namedTag.getBoolean("Sheared");
		}

		if (packet == null) {
			packet = new EntityEventPacket();
			packet.eid = this.getId();
			packet.event = EntityEventPacket.EAT_GRASS_ANIMATION;
		}

		this.nextEatGlass = Server.getInstance().getTick() + Utils.rand(20*60,20*120);

		this.setDataFlag(DATA_FLAGS, DATA_FLAG_SHEARED, this.sheared);
	}

	public void setColor(int color) {
		this.color = color;
		this.setDataProperty(new ByteEntityData(DATA_COLOUR, color));
		this.namedTag.putByte("Color", this.color);
	}

	public int getColor() {
		return namedTag.getByte("Color");
	}

	private int randomColor() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		double rand = random.nextDouble(1, 100);

		if (rand <= 0.164) {
			return DyeColor.PINK.getWoolData();
		}

		if (rand <= 15) {
			return random.nextBoolean() ? DyeColor.BLACK.getWoolData() : random.nextBoolean() ? DyeColor.GRAY.getWoolData() : DyeColor.LIGHT_GRAY.getWoolData();
		}

		return DyeColor.WHITE.getWoolData();
	}

	public boolean shear() {
		if (sheared) {
			return false;
		}

		this.sheared = true;
		this.setDataFlag(DATA_FLAGS, DATA_FLAG_SHEARED, true);

		this.level.dropItem(this, Item.get(Item.WOOL, getColor(), this.level.rand.nextInt(2) + 1));

		this.addHook("Sheared", new SheepWoolHook(this, Utils.rand(20 * 60 * 3, 20 * 60 * 10))); // After 3-10 minutes, the sheep's wool will grow back
		return true;
	}

	@Override
	public void saveNBT() {
		super.saveNBT();

		this.namedTag.putByte("Color", this.color);
		this.namedTag.putBoolean("Sheared", this.sheared);
	}

}
