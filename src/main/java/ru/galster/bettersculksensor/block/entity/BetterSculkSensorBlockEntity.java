package ru.galster.bettersculksensor.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.GameEventListener;
import net.minecraft.world.event.listener.VibrationListener;
import org.jetbrains.annotations.Nullable;
import ru.galster.bettersculksensor.ModBlocks;
import ru.galster.bettersculksensor.block.BetterSculkSensorBlock;

import java.util.Arrays;

import static ru.galster.bettersculksensor.block.BetterSculkSensorBlock.VIBRATION_EVENT;

public class BetterSculkSensorBlockEntity extends BlockEntity implements VibrationListener.Callback {
    private final BetterVibrationListener listener;
    @Nullable
    private ItemStack clickedItem;
    private boolean shouldRenderFlat;
    private int angle;

    public BetterSculkSensorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BETTER_SCULK_SENSOR_BLOCK_ENTITY, pos, state);
        this.listener = new BetterVibrationListener(((BetterSculkSensorBlock)state.getBlock()).getRange(), this, null, 0.0F, 0, this);
    }

    public BetterVibrationListener getEventListener() {
        return this.listener;
    }

    public @Nullable ItemStack getClickedItem() {
        return clickedItem;
    }

    public void setClickedItem(@Nullable ItemStack clickedItem) {
        this.clickedItem = clickedItem;
    }

    public boolean shouldRenderFlat() {
        return shouldRenderFlat;
    }

    public void setShouldRenderFlat(boolean shouldRenderFlat) {
        this.shouldRenderFlat = shouldRenderFlat;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    @Override
    public boolean triggersAvoidCriterion() {
        return true;
    }

    @Override
    public boolean accepts(ServerWorld world, GameEventListener listener, BlockPos pos, GameEvent event, @Nullable GameEvent.Emitter emitter) {
        // check if we want to get triggered by a game event

        return  !world.isClient
                && !this.isRemoved()
                && (!pos.equals(this.getPos()) || event != GameEvent.BLOCK_PLACE && event != GameEvent.BLOCK_DESTROY)
                && BetterSculkSensorBlock.isInactive(this.getCachedState())
                // the current tune is saved in a block and not BE because it's easier to maintain and visible in F3 debug menu
                && event.getId().equals(this.getCachedState().get(VIBRATION_EVENT).asString());
    }

    @Override
    public void accept(ServerWorld world, GameEventListener listener, BlockPos pos, GameEvent event, @Nullable Entity entity, @Nullable Entity sourceEntity, float distance) {
        BlockState blockState = this.getCachedState();
        if (BetterSculkSensorBlock.isInactive(blockState)) {
            BetterSculkSensorBlock.setActive(entity, world, this.pos, blockState, getPower(distance, listener.getRange()));
        }
    }

    @Override
    public void onListen() {
        this.markDirty();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        // why the heck does original sculk sensor saves listener in nbt
        // it breaks so much stuff
        var vibrationEventId = nbt.getString("vibration_event");
        var newState = this.getCachedState()
                .with(VIBRATION_EVENT,
                        Arrays.stream(BetterSculkSensorBlock.BetterVibration.values())
                                .filter(v -> vibrationEventId.equals(v.asString()))
                                .findAny()
                                .orElse(BetterSculkSensorBlock.BetterVibration.NONE)
                );

        if(nbt.contains("clicked_item", 10)) {
            ItemStack.CODEC
                    .parse(NbtOps.INSTANCE, nbt.getCompound("clicked_item"))
                    .resultOrPartial(LogUtils.getLogger()::error)
                    .ifPresent(i -> this.clickedItem = i);
        }

        this.shouldRenderFlat = nbt.getBoolean("flat");
        this.angle = nbt.getInt("additional_angle");

        // my IDEA cries about it possibly being null IDK what's it talking about
        if(this.world == null) {
            // should never be called
            this.setCachedState(newState);
        } else {
            this.world.setBlockState(this.getPos(), newState);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.putString("vibration_event", this.getCachedState().get(VIBRATION_EVENT).asString());
        nbt.putBoolean("flat", this.shouldRenderFlat);
        nbt.putInt("additional_angle", this.angle);

        if(this.clickedItem != null) {
            ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, this.clickedItem)
                    .resultOrPartial(LogUtils.getLogger()::error)
                    .ifPresent(i -> nbt.put("clicked_item", i));
        }
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    public static int getPower(float distance, int range) {
        double d = (double)distance / (double)range;
        return Math.max(1, 15 - MathHelper.floor(d * 15.0));
    }
}
