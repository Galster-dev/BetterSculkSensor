package ru.galster.bettersculksensor.block.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.VibrationParticleEffect;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockStateRaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.listener.VibrationListener;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class BetterVibrationListener extends VibrationListener {
    public BetterVibrationListener(int range, Callback callback, BetterSculkSensorBlockEntity blockEntity) {
        super(null, range, callback);
        this.blockEntity = blockEntity;
    }

    protected Vibration vibration;
    private final BetterSculkSensorBlockEntity blockEntity;

    public void actualTick(World world) {
        if (world instanceof ServerWorld serverWorld) {
            if (this.vibration != null) {
                this.callback.accept(serverWorld, this, BlockPos.ofFloored(this.vibration.pos), this.vibration.gameEvent, this.vibration.getEntity(serverWorld).orElse(null), this.vibration.getOwner(serverWorld).orElse(null), this.getRange());
                this.vibration = null;
            }
        }
    }

    @Override
    public void tick(World world) {

    }

    @Override
    public PositionSource getPositionSource() {
        return new BlockPositionSource(this.blockEntity.getPos());
    }

    @Override
    public boolean listen(ServerWorld world, GameEvent event, GameEvent.Emitter emitter, Vec3d emmiterPos) {
        if (this.vibration != null) {
            return false;
        }
        if (!this.callback.canAccept(event, emitter)) {
            return false;
        }
        Optional<Vec3d> optional = this.getPositionSource().getPos(world);
        if (optional.isEmpty()) {
            return false;
        }
        Vec3d vec3d2 = optional.get();
        if (!this.callback.accepts(world, this, BlockPos.ofFloored(emmiterPos), event, emitter)) {
            return false;
        }
        if (isOccluded(world, emmiterPos, vec3d2)) {
            return false;
        }
        this.listen(world, event, emitter, emmiterPos, vec3d2);
        return true;
    }

    private void listen(ServerWorld world, GameEvent gameEvent, GameEvent.Emitter emitter, Vec3d start, Vec3d end) {
        var distance = (float)start.distanceTo(end);
        this.vibration = new Vibration(gameEvent, distance, start, emitter.sourceEntity());
        this.delay = MathHelper.floor(distance);
        world.spawnParticles(new VibrationParticleEffect(this.getPositionSource(), this.delay), start.x, start.y, start.z, 1, 0.0, 0.0, 0.0, 0.0);
        var blockPos = this.blockEntity.getPos();
        world.scheduleBlockTick(blockPos, world.getBlockState(blockPos).getBlock(), this.delay);
        this.callback.onListen();
    }

    private static boolean isOccluded(World world, Vec3d start, Vec3d end) {
        Vec3d startPos = new Vec3d((double)MathHelper.floor(start.x) + 0.5, (double)MathHelper.floor(start.y) + 0.5, (double)MathHelper.floor(start.z) + 0.5);
        Vec3d endPos = new Vec3d((double)MathHelper.floor(end.x) + 0.5, (double)MathHelper.floor(end.y) + 0.5, (double)MathHelper.floor(end.z) + 0.5);

        for(var direction : Direction.values()) {
            Vec3d vec3d3 = startPos.offset(direction, 9.999999747378752E-6);
            if (world.raycast(new BlockStateRaycastContext(vec3d3, endPos, (state) -> state.isIn(BlockTags.OCCLUDES_VIBRATION_SIGNALS))).getType() != HitResult.Type.BLOCK) {
                return false;
            }
        }

        return true;
    }

    public record Vibration(GameEvent gameEvent, float distance, Vec3d pos, @Nullable UUID uuid, @Nullable UUID projectileOwnerUuid, @Nullable Entity entity) {
        public Vibration(GameEvent gameEvent, float distance, Vec3d pos, @Nullable Entity entity) {
            this(gameEvent, distance, pos, entity == null ? null : entity.getUuid(), getOwnerUuid(entity), entity);
        }

        @Nullable
        private static UUID getOwnerUuid(@Nullable Entity entity) {
            ProjectileEntity projectileEntity;
            if (entity instanceof ProjectileEntity && (projectileEntity = (ProjectileEntity)entity).getOwner() != null) {
                return projectileEntity.getOwner().getUuid();
            }
            return null;
        }

        public Optional<Entity> getEntity(ServerWorld world) {
            return Optional.ofNullable(this.entity).or(() -> Optional.ofNullable(this.uuid).map(world::getEntity));
        }

        public Optional<Entity> getOwner(ServerWorld world) {
            return this.getEntity(world).filter(entity -> entity instanceof ProjectileEntity).map(entity -> (ProjectileEntity)entity).map(ProjectileEntity::getOwner).or(() -> Optional.ofNullable(this.projectileOwnerUuid).map(world::getEntity));
        }
    }
}
