package me.onebone.actaeon.entity;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockLiquid;
import cn.nukkit.entity.Attribute;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.UpdateAttributesPacket;
import me.onebone.actaeon.hook.MovingEntityHook;
import me.onebone.actaeon.route.AdvancedRouteFinder;
import me.onebone.actaeon.route.Node;
import me.onebone.actaeon.route.RouteFinder;
import me.onebone.actaeon.runnable.RouteFinderSearchAsyncTask;
import me.onebone.actaeon.target.TargetFinder;
import me.onebone.actaeon.task.MovingEntityTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

abstract public class MovingEntity extends EntityCreature {

    private boolean isKnockback = false;
    private RouteFinder route = null;
    private TargetFinder targetFinder = null;
    private Vector3 target = null;
    private Entity hate = null;
    private String targetSetter = "";
    public boolean routeLeading = true;
    private Map<String, MovingEntityHook> hooks = new HashMap<>();
    private MovingEntityTask task = null;
    private boolean lookAtFront = true;

    public MovingEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

        this.route = new AdvancedRouteFinder(this);
        //this.route = new SimpleRouteFinder(this);
        this.setImmobile(false);
    }


    public void setBaby(boolean isBaby) {
        this.setDataFlag(DATA_FLAGS, Entity.DATA_FLAG_BABY, isBaby);
    }

    public Map<String, MovingEntityHook> getHooks() {
        return hooks;
    }

    public void addHook(String key, MovingEntityHook hook) {
        this.hooks.put(key, hook);
    }

    public void removeHook(String key) {
        this.hooks.remove(key);
    }

    @Override
    protected float getGravity() {
        return 0.092f;
    }

    public Entity getHate() {
        return hate;
    }

    public void setHate(Entity hate) {
        this.hate = hate;
    }

    public void jump() {
        if (this.onGround) {
            this.motionY = Math.sqrt(2 * getJumpHeight() * getGravity());
        }
    }

    public double getJumpHeight() {
        return 1.25;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        super.onUpdate(currentTick);
        return true;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (this.closed) {
            return false;
        }
        if (hooks != null) {
            new ArrayList<>(this.hooks.values()).forEach(
                    hook -> hook.onUpdate(Server.getInstance().getTick())
            );
        } else {
            hooks = new HashMap<>();
        }
        if (this.task != null) this.task.onUpdate(Server.getInstance().getTick());

        boolean hasUpdate = super.entityBaseTick(tickDiff);

        if (this.isKnockback) {    // knockback 이 true 인 경우는 맞은 직후

        } else if (this.routeLeading && this.onGround) {
            this.motionX = this.motionZ = 0;
        }

        this.motionX *= (1 - this.getDrag());
        this.motionZ *= (1 - this.getDrag());

        if (this.targetFinder != null) this.targetFinder.onUpdate();

        if (this.routeLeading && this.onGround && this.hasSetTarget() && !this.route.isSearching() && System.currentTimeMillis() >= this.route.stopRouteFindUntil && (this.route.getDestination() == null || this.route.getDestination().distance(this.getTarget()) > 2)) { // 대상이 이동함
            if (RouteFinderSearchAsyncTask.getTaskSize() < 50)
                Server.getInstance().getScheduler().scheduleAsyncTask(new RouteFinderSearchAsyncTask(this.route, this.level, this, this.getTarget(), this.boundingBox));

			/*if(this.route.isSearching()) this.route.research();
			else this.route.search();*/

            hasUpdate = true;
        }

        if (!this.isImmobile()) {
            if (this.routeLeading && !this.isKnockback && !this.route.isSearching() && this.route.isSuccess() && this.route.hasRoute() && this.route.hasNext()) { // entity has route to go
                hasUpdate = true;

                Node node = this.route.get();
                if (node != null) {
                    //level.addParticle(new cn.nukkit.level.particle.RedstoneParticle(node.getVector3(), 2));
                    Vector3 vec = node.getVector3();
                    double diffX = Math.pow(vec.x - this.x, 2);
                    double diffZ = Math.pow(vec.z - this.z, 2);

                    if (diffX + diffZ == 0) {
                        if (!this.route.next()) {
                            this.route.arrived();
                            //Server.getInstance().getLogger().warning(vec.toString());
                        }
                    } else {
                        int negX = vec.x - this.x < 0 ? -1 : 1;
                        int negZ = vec.z - this.z < 0 ? -1 : 1;

                        this.motionX = Math.min(Math.abs(vec.x - this.x), diffX / (diffX + diffZ) * this.getMovementSpeed()) * negX;
                        this.motionZ = Math.min(Math.abs(vec.z - this.z), diffZ / (diffX + diffZ) * this.getMovementSpeed()) * negZ;
                        if (this.lookAtFront) {
                            double angle = Math.atan2(vec.z - this.z, vec.x - this.x);
                            this.setRotation((angle * 180) / Math.PI - 90, 0);
                        }
                    }
                }
            }

            for (Entity entity : this.getLevel().getCollidingEntities(this.boundingBox)) {
                if (this.canCollide() && this.canCollideWith(entity)) {
                    Vector3 motion = this.subtract(entity);
                    this.motionX += motion.x / 2;
                    this.motionZ += motion.z / 2;
                }
            }

            double swim = 0;

            for (Block block : getCollisionBlocks()) {
                if (block instanceof BlockLiquid) {
                    float f = ((BlockLiquid) block).getFluidHeightPercent();
                    double minY = block.getFloorY();
                    double maxY = minY + 1 - f;

                    swim = Math.max(swim, this.boundingBox.getMaxX() >= maxY ? maxY - this.boundingBox.getMinY() : this.boundingBox.getMaxZ() - minY);
                    break;
                }
            }

            if (swim != 0) {
                if (ThreadLocalRandom.current().nextFloat() < 0.8) {
                    this.motionY = Math.min(0.2, this.motionY + (0.3 * swim));
                }
            }

            if ((this.motionX != 0 || this.motionZ != 0) && this.isCollidedHorizontally) {
                AxisAlignedBB bb = this.boundingBox.clone().offset(this.motionX, this.motionY, this.motionZ);
                Block[] blocks = this.level.getCollisionBlocks(bb);

                boolean jump = false;
                boolean step = true;

                for (Block b : blocks) {
                    AxisAlignedBB blockBB = b.getBoundingBox();
                    if (blockBB == null || b.canPassThrough())
                        continue;

                    double diffY = blockBB.getMaxY() - this.boundingBox.getMinY();
                    if (diffY < getJumpHeight()) {
                        jump = true;
                    }

                    if (step)
                        step = diffY <= getStepHeight();
                }

                if (jump && !step) {
                    this.jump();
                }
            }
            this.move(this.motionX, this.motionY, this.motionZ);

            this.checkGround();
            if (!this.onGround && swim == 0) {
                this.motionY -= this.getGravity();
                //Server.getInstance().getLogger().warning(this.getId() + ": 不在地面, 掉落 motionY=" + this.motionY);
                hasUpdate = true;
            } else {
                this.isKnockback = false;
            }
        }


        return hasUpdate;
    }

    public double getRange() {
        return 100.0;
    }

    public void setTarget(Vector3 vec, String identifier) {
        this.setTarget(vec, identifier, false);
    }

    public void setTarget(Vector3 vec, String identifier, boolean forceSearch) {
        if (identifier == null) return;

        if (forceSearch || !this.hasSetTarget() || identifier.equals(this.targetSetter)) {
            this.target = vec;

            this.targetSetter = identifier;
        }

        if (this.hasSetTarget() && (forceSearch || !this.route.hasRoute())) {
            this.route.forceStop();
            Server.getInstance().getScheduler().scheduleAsyncTask(new RouteFinderSearchAsyncTask(this.route, this.level, this, this.getTarget(), this.boundingBox.clone()));
			/*if(this.route.isSearching()) this.route.research();
			else this.route.search();*/
        }
    }

    public Vector3 getRealTarget() {
        return this.target;
    }

    public Vector3 getTarget() {
        return new Vector3(this.target.x, this.target.y, this.target.z);
    }

    /**
     * Returns whether the entity has following target
     * Entity will try to move to position where target exists
     */
    public boolean hasFollowingTarget() {
        return this.route.getDestination() != null && this.target != null && this.distance(this.target) < this.getRange();
    }

    /**
     * Returns whether the entity has set its target
     * The entity may not follow the target if there is following target and set target is different
     * If following distance of target is too far to follow or cannot reach, set target will be the next following target
     */
    public boolean hasSetTarget() {
        return this.target != null && this.distance(this.target) < this.getRange();
    }

    @Override
    protected void checkGroundState(double movX, double movY, double movZ, double dx, double dy, double dz) {
        this.isCollidedVertically = movY != dy;
        this.isCollidedHorizontally = (movX != dx || movZ != dz);
        this.isCollided = (this.isCollidedHorizontally || this.isCollidedVertically);

        // this.onGround = (movY != dy && movY < 0);
        // onGround 는 onUpdate 에서 확인
    }

    private void checkGround() {
        AxisAlignedBB[] list = this.level.getCollisionCubes(this, this.level.getTickRate() > 1 ? this.boundingBox.getOffsetBoundingBox(0, -1, 0) : this.boundingBox.addCoord(0, -1, 0), false);

        double maxY = 0;
        for (AxisAlignedBB bb : list) {
            if (bb.getMaxY() > maxY) {
                maxY = bb.getMaxY();
            }
        }

        this.onGround = (maxY == this.boundingBox.getMinY());
    }

    @Override
    public void setHealth(float health) {
        super.setHealth(health);
        UpdateAttributesPacket pk0 = new UpdateAttributesPacket();
        pk0.entityId = this.getId();
        pk0.entries = new Attribute[]{
                Attribute.getAttribute(Attribute.MAX_HEALTH).setMaxValue(this.getMaxHealth()).setValue(this.getHealth()),
        };
        this.getLevel().addChunkPacket(this.chunk.getX(), this.chunk.getZ(), pk0);
    }

    @Override
    public void setMaxHealth(int maxHealth) {
        super.setMaxHealth(maxHealth);
        if (this.getHealth() > maxHealth) this.health = maxHealth;
        UpdateAttributesPacket pk0 = new UpdateAttributesPacket();
        pk0.entityId = this.getId();
        pk0.entries = new Attribute[]{
                Attribute.getAttribute(Attribute.MAX_HEALTH).setMaxValue(this.getMaxHealth()).setValue(this.getHealth()),
        };
        this.getLevel().addChunkPacket(this.chunk.getX(), this.chunk.getZ(), pk0);
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.setDataFlag(Entity.DATA_FLAGS, Entity.DATA_FLAG_NO_AI);
    }

    @Override
    public void knockBack(Entity attacker, double damage, double x, double z, double base) {
        this.isKnockback = true;

        super.knockBack(attacker, damage, x, z, base / 2);
    }

    public void setRoute(RouteFinder route) {
        this.route = route;
    }

    public RouteFinder getRoute() {
        return route;
    }

    public TargetFinder getTargetFinder() {
        return targetFinder;
    }

    public void setTargetFinder(TargetFinder targetFinder) {
        this.targetFinder = targetFinder;
    }

    public void updateBotTask(MovingEntityTask task) {
        if (this.task != null) this.task.forceStop();
        this.task = task;
        if (task != null) this.task.onUpdate(Server.getInstance().getTick());
    }

    public MovingEntityTask getTask() {
        return task;
    }

    public boolean isLookAtFront() {
        return lookAtFront;
    }

    public void setLookAtFront(boolean lookAtFront) {
        this.lookAtFront = lookAtFront;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        new ArrayList<>(this.hooks.values()).forEach(hook -> hook.onDamage(source));

        return super.attack(source);
    }

    @Override
    protected double getStepHeight() {
        return 0.6;
    }
}

