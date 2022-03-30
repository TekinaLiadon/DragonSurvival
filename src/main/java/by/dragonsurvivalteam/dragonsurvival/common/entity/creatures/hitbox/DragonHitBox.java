package by.dragonsurvivalteam.dragonsurvival.common.entity.creatures.hitbox;

import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateHandler;
import by.dragonsurvivalteam.dragonsurvival.common.capability.provider.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.handlers.DragonSizeHandler;
import by.dragonsurvivalteam.dragonsurvival.common.util.DragonUtils;
import by.dragonsurvivalteam.dragonsurvival.config.ConfigHandler;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

public class DragonHitBox extends Mob{
	public static final EntityDataAccessor<Integer> PLAYER_ID = SynchedEntityData.defineId(DragonHitBox.class, EntityDataSerializers.INT);

	private final DragonHitboxPart[] subEntities;
	private final DragonHitboxPart head;
	private final DragonHitboxPart body;
	private final DragonHitboxPart tail1;
	private final DragonHitboxPart tail2;
	private final DragonHitboxPart tail3;
	private final DragonHitboxPart tail4;
	private final DragonHitboxPart tail5;
	public EntityDimensions size;
	public Player player;
	private double lastSize;
	private Pose lastPose;

	public DragonHitBox(EntityType<? extends Mob> p_i48577_1_, Level p_i48580_2_){
		super(p_i48577_1_, p_i48580_2_);

		this.head = new DragonHitboxPart(this, "head", 0.5F, 0.5F);
		this.body = new DragonHitboxPart(this, "body", 0.5F, 0.5F);
		this.tail1 = new DragonHitboxPart(this, "tail1", 1.0F, 1.0F);
		this.tail2 = new DragonHitboxPart(this, "tail2", 1.0F, 1.0F);
		this.tail3 = new DragonHitboxPart(this, "tail3", 1.0F, 1.0F);
		this.tail4 = new DragonHitboxPart(this, "tail4", 1.0F, 1.0F);
		this.tail5 = new DragonHitboxPart(this, "tail5", 1.0F, 1.0F);

		this.subEntities = new DragonHitboxPart[]{this.head, this.body, this.tail1, this.tail2, this.tail3, this.tail4, this.tail5};

		this.size = EntityDimensions.scalable(1f, 1f);
		this.refreshDimensions();
	}

	@Override
	protected void defineSynchedData(){
		super.defineSynchedData();
		this.entityData.define(PLAYER_ID, -1);
	}

	@Override
	public void tick(){
		super.tick();
		this.checkInsideBlocks();
	}

	@Override
	public void aiStep(){
		if(level.isClientSide && tickCount > 20){
			if(player == null){
				if(getPlayerId() != -1){
					Entity ent = level.getEntity(getPlayerId());

					if(ent instanceof Player){
						player = (Player)ent;
					}
				}
				return;
			}
		}else{
			if(player == null || player.isDeadOrDying() || !DragonUtils.isDragon(player)){
				if(!level.isClientSide){
					this.remove(RemovalReason.DISCARDED);
				}
			}
		}


		DragonStateHandler handler = DragonStateProvider.getCap(player).orElse(null);

		if(handler == null || handler.getMovementData() == null){
			return;
		}
		Vector3f offset = DragonUtils.getCameraOffset(player);

		double size = handler.getSize();
		double height = DragonSizeHandler.calculateDragonHeight(size, ConfigHandler.SERVER.hitboxGrowsPastHuman.get());
		double width = DragonSizeHandler.calculateDragonWidth(size, ConfigHandler.SERVER.hitboxGrowsPastHuman.get());

		Pose overridePose = DragonSizeHandler.overridePose(player);
		height = DragonSizeHandler.calculateModifiedHeight(height, overridePose, true);


		double headRot = handler.getMovementData().headYaw;
		double pitch = handler.getMovementData().headPitch * -1;
		Vector3f bodyRot = DragonUtils.getCameraOffset(player);

		bodyRot = new Vector3f(bodyRot.x() / 2, bodyRot.y() / 2, bodyRot.z() / 2);

		Point2D result = new Point2D.Double();
		Point2D result2 = new Point2D.Double();

		{
			Point2D point = new Double(player.position().x() + bodyRot.x(), player.position().y() + player.getEyeHeight());
			AffineTransform transform = new AffineTransform();
			double angleInRadians = ((Mth.clamp(pitch, -90, 90) * -1) * Math.PI / 360);
			transform.rotate(angleInRadians, player.position().x(), player.position().y() + player.getEyeHeight());
			transform.transform(point, result);
		}

		{
			Point2D point2 = new Double(player.position().x() + bodyRot.x(), player.position().z() + bodyRot.z());
			AffineTransform transform2 = new AffineTransform();
			double angleInRadians2 = ((Mth.clamp(headRot, -180, 180) * -1) * Math.PI / 360);
			transform2.rotate(angleInRadians2, player.position().x(), player.position().z());
			transform2.transform(point2, result2);
		}

		double dx = result2.getX();
		double dy = result.getY() - (Math.abs(headRot) / 180 * .5);
		double dz = result2.getY();

		if(lastSize != size || lastPose != overridePose){
			this.size = EntityDimensions.scalable((float)width * 1.6f, (float)height);
			refreshDimensions();

			body.size = EntityDimensions.scalable((float)width * 1.6f, (float)height);
			body.refreshDimensions();

			head.size = EntityDimensions.scalable((float)width, (float)width);
			head.refreshDimensions();

			tail1.size = EntityDimensions.scalable((float)width, (float)height / 3);
			tail1.refreshDimensions();

			tail2.size = EntityDimensions.scalable((float)width * 0.8f, (float)height / 3);
			tail2.refreshDimensions();

			tail3.size = EntityDimensions.scalable((float)width * 0.7f, (float)height / 3);
			tail3.refreshDimensions();

			tail4.size = EntityDimensions.scalable((float)width * 0.7f, (float)height / 3);
			tail4.refreshDimensions();

			tail5.size = EntityDimensions.scalable((float)width * 0.7f, (float)height / 3);
			tail5.refreshDimensions();

			lastSize = size;
			lastPose = overridePose;
			player.refreshDimensions();
		}else{
			setPos(player.getX() - offset.x(), player.getY(), player.getZ() - offset.z());
			setXRot((float)handler.getMovementData().headPitch);
			setYRot((float)handler.getMovementData().bodyYaw);

			body.setPos(player.getX() - offset.x(), player.getY(), player.getZ() - offset.z());
			head.setPos(dx, dy - (DragonSizeHandler.calculateDragonWidth(handler.getSize(), ConfigHandler.SERVER.hitboxGrowsPastHuman.get()) / 2), dz);
			tail1.setPos(getX() - offset.x(), getY() + (player.getEyeHeight() / 2) - (height / 9), getZ() - offset.z());
			tail2.setPos(getX() - offset.x() * 1.5, getY() + (player.getEyeHeight() / 2) - (height / 9), getZ() - offset.z() * 1.5);
			tail3.setPos(getX() - offset.x() * 2, getY() + (player.getEyeHeight() / 2) - (height / 9), getZ() - offset.z() * 2);
			tail4.setPos(getX() - offset.x() * 2.5, getY() + (player.getEyeHeight() / 2) - (height / 9), getZ() - offset.z() * 2.5);
			tail5.setPos(getX() - offset.x() * 3, getY() + (player.getEyeHeight() / 2) - (height / 9), getZ() - offset.z() * 3);
		}
	}

	public int getPlayerId(){
		return this.entityData.get(PLAYER_ID);
	}

	public void setPlayerId(int id){
		this.entityData.set(PLAYER_ID, id);
	}

	public boolean hurt(DragonHitboxPart part, DamageSource source, float damage){
		return hurt(source, damage);
	}

	@Override
	public boolean hurt(DamageSource source, float damage){
		if(source.getEntity() == player || source.getDirectEntity() == player) return false;
		return player != null && !this.isInvulnerableTo(source) && player.hurt(source, damage);
	}

	@Override
	public boolean isPickable(){
		return level.isClientSide && !isOwner();
	}

	@OnlyIn( Dist.CLIENT )
	public boolean isOwner(){
		return player == Minecraft.getInstance().player;
	}

	@Override
	public Packet<?> getAddEntityPacket(){
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public EntityDimensions getDimensions(Pose pPose){
		return size;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource pSource){
		return super.isInvulnerableTo(pSource) || pSource == DamageSource.IN_WALL || pSource == DamageSource.LIGHTNING_BOLT || player != null && player.isInvulnerableTo(pSource);
	}

	@Override
	public int getAirSupply(){
		return getMaxAirSupply();
	}

	@Override
	public Vec3 getDeltaMovement(){
		return player != null ? player.getDeltaMovement() : super.getDeltaMovement();
	}

	@Override
	public boolean isMultipartEntity(){
		return true;
	}

	@Nullable
	@Override
	public PartEntity<?>[] getParts(){
		return subEntities;
	}

	@Override
	public boolean fireImmune(){
		return player != null ? player.fireImmune() : super.fireImmune();
	}

	@Override
	public boolean isOnFire(){
		return false;
	}

	public boolean is(Entity entity){
		return this == entity || entity.getId() == getPlayerId() || player != null && entity.getId() == player.getId();
	}
}