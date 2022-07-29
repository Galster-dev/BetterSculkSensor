package ru.galster.bettersculksensor.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.SculkSensorPhase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Items;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;
import ru.galster.bettersculksensor.ModBlocks;
import ru.galster.bettersculksensor.block.entity.BetterSculkSensorBlockEntity;

import java.util.Arrays;
import java.util.List;

public class BetterSculkSensorBlock extends BlockWithEntity implements Waterloggable {
    // excluding pulse length
    public static final int COOLDOWN = 6;
    public static final int PULSE_LENGTH = 2;
    public static final BetterVibration[] VIBRATIONS = BetterVibration.values();
    public static final EnumProperty<BetterVibration> VIBRATION_EVENT = EnumProperty.of("vibration_event", BetterVibration.class);
    public static final IntProperty POWER = Properties.POWER;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final EnumProperty<SculkSensorPhase> SCULK_SENSOR_PHASE = Properties.SCULK_SENSOR_PHASE;
    protected static final VoxelShape OUTLINE_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private final int range;

    public int getRange() {
        return this.range;
    }

    public BetterSculkSensorBlock(Settings settings, int range) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(SCULK_SENSOR_PHASE, SculkSensorPhase.INACTIVE).with(POWER, 0).with(WATERLOGGED, false));
        this.range = range;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BetterSculkSensorBlockEntity(pos, state);
    }

    // strong power blocks in all directions
    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWER);
    }

    // wipe comparator frequency mechanic
    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return false;
    }

    // just in case
    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return state.get(POWER);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        // empty because hueta
    }

    // the current tuning method
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(world.isClient) {
            return ActionResult.SUCCESS;
        }

        // do not break waterlogging
        if(player.isSneaking()) {
            return ActionResult.PASS;
        }

        var itemInHand = player.getStackInHand(hand).getItem();
        // find a vibration that can be configured with clicked item
        var newVibration = Arrays.stream(VIBRATIONS)
                .filter(v -> v.getConfigureItems().contains(itemInHand))
                .findFirst();

        if(newVibration.isEmpty()) {
            return ActionResult.PASS;
        }

        var actualNewVibration = newVibration.get();
        var blockEntity = (BetterSculkSensorBlockEntity)world.getBlockEntity(pos);
        assert blockEntity != null;
        blockEntity.setClickedItem(itemInHand.getDefaultStack());
        blockEntity.setShouldRenderFlat(actualNewVibration.shouldRenderFlat());
        blockEntity.setAngle(actualNewVibration.getRotateDegree());

        state = state.with(VIBRATION_EVENT, actualNewVibration);
        world.setBlockState(pos, state, 3);
        if (!state.get(WATERLOGGED)) {
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.BLOCKS, 1f, 0f);
        }

        return ActionResult.CONSUME;
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        switch (getPhase(state)) {
            case ACTIVE -> {
                world.setBlockState(pos, state.with(SCULK_SENSOR_PHASE, SculkSensorPhase.COOLDOWN).with(POWER, 0), 3);
                world.createAndScheduleBlockTick(pos, state.getBlock(), COOLDOWN);
                if (!state.get(WATERLOGGED)) {
                    world.playSound(null, pos, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING_STOP, SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.2F + 0.8F);
                }

                updateNeighbors(world, pos);
            }
            case COOLDOWN -> {
                world.setBlockState(pos, state.with(SCULK_SENSOR_PHASE, SculkSensorPhase.INACTIVE).with(POWER, 0), 3);
                updateNeighbors(world, pos);
            }
            case INACTIVE -> {
                if(world.getBlockEntity(pos) instanceof BetterSculkSensorBlockEntity blockEntity) {
                    blockEntity.getEventListener().actualTick(world);
                }
            }
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (getPhase(state) != SculkSensorPhase.INACTIVE) {
                updateNeighbors(world, pos);
            }
        }

        // this is the line why I had to rewrite whole block and cannot extend SculkSensorBlock
        // I cannot call double super (afaik) but parent BE handing has to be *called* and not *pasted* for patches like carpet work properly
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos blockPos = ctx.getBlockPos();
        FluidState fluidState = ctx.getWorld().getFluidState(blockPos);
        return this.getDefaultState().with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> GameEventListener getGameEventListener(ServerWorld world, T blockEntity) {
        return blockEntity instanceof BetterSculkSensorBlockEntity ? ((BetterSculkSensorBlockEntity)blockEntity).getEventListener() : null;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return !world.isClient ? checkType(type, ModBlocks.BETTER_SCULK_SENSOR_BLOCK_ENTITY, (w, p, s, e) -> e.getEventListener().tick(w)) : null;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.createAndScheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient() && !state.isOf(oldState.getBlock())) {
            if (state.get(POWER) > 0 && !world.getBlockTickScheduler().isQueued(pos, this)) {
                world.setBlockState(pos, state.with(POWER, 0), 18);
            }

            world.createAndScheduleBlockTick(new BlockPos(pos), state.getBlock(), 1);
        }
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWER);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (getPhase(state) == SculkSensorPhase.ACTIVE) {
            Direction direction = Direction.random(random);
            if (direction != Direction.UP && direction != Direction.DOWN) {
                var d = pos.getX() + 0.5 + (direction.getOffsetX() == 0 ? 0.5 - random.nextDouble() : direction.getOffsetX() * 0.6);
                var e = pos.getY() + 0.25;
                var f = pos.getZ() + 0.5 + (direction.getOffsetZ() == 0 ? 0.5 - random.nextDouble() : direction.getOffsetZ() * 0.6);
                var g = random.nextFloat() * 0.04;
                world.addParticle(DustColorTransitionParticleEffect.DEFAULT, d, e, f, 0.0, g, 0.0);
            }
        }
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    @Override
    public boolean hasSidedTransparency(BlockState state) {
        return true;
    }

    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(SCULK_SENSOR_PHASE, POWER, WATERLOGGED, VIBRATION_EVENT);
    }

    public static void updateNeighbors(World world, BlockPos pos) {
        world.updateNeighborsAlways(pos, ModBlocks.BETTER_SCULK_SENSOR_BLOCK);
        // update blocks in all directions because now we strong power them
        for (Direction direction : Direction.values()) {
            world.updateNeighborsAlways(pos.offset(direction), ModBlocks.BETTER_SCULK_SENSOR_BLOCK);
        }
    }

    public static void setActive(@Nullable Entity entity, World world, BlockPos pos, BlockState state, int power) {
        world.setBlockState(pos, state.with(SCULK_SENSOR_PHASE, SculkSensorPhase.ACTIVE).with(POWER, power), 3);
        world.createAndScheduleBlockTick(pos, state.getBlock(), PULSE_LENGTH);
        updateNeighbors(world, pos);
        world.emitGameEvent(entity, GameEvent.SCULK_SENSOR_TENDRILS_CLICKING, pos);

        if (!state.get(WATERLOGGED)) {
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.2F + 0.8F);
        }
    }

    public static SculkSensorPhase getPhase(BlockState state) {
        return state.get(SCULK_SENSOR_PHASE);
    }

    public static boolean isInactive(BlockState state) {
        return getPhase(state) == SculkSensorPhase.INACTIVE;
    }

    public enum BetterVibration implements StringIdentifiable {
        NONE(null, false, Items.AIR),
        STEP(GameEvent.STEP, true, Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.GOLDEN_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS),
        FLAP(GameEvent.FLAP, true, Items.PHANTOM_MEMBRANE),
        SWIM(GameEvent.SWIM, false, Items.CONDUIT),
        ELYTRA_GLIDE(GameEvent.ELYTRA_GLIDE, true, Items.ELYTRA),
        HIT_GROUND(GameEvent.HIT_GROUND, false, Items.ANVIL, Items.CHIPPED_ANVIL, Items.DAMAGED_ANVIL, Items.SAND, Items.GRAVEL, Items.BLACK_CONCRETE_POWDER, Items.BLUE_CONCRETE_POWDER, Items.BROWN_CONCRETE_POWDER, Items.BROWN_CONCRETE_POWDER, Items.CYAN_CONCRETE_POWDER, Items.GRAY_CONCRETE_POWDER, Items.GREEN_CONCRETE_POWDER, Items.LIME_CONCRETE_POWDER, Items.MAGENTA_CONCRETE_POWDER, Items.ORANGE_CONCRETE_POWDER, Items.PINK_CONCRETE_POWDER, Items.PURPLE_CONCRETE_POWDER, Items.RED_CONCRETE_POWDER, Items.YELLOW_CONCRETE_POWDER, Items.LIGHT_BLUE_CONCRETE_POWDER, Items.LIGHT_GRAY_CONCRETE_POWDER),
        TELEPORT(GameEvent.TELEPORT, true, Items.ENDER_PEARL),
        SPLASH(GameEvent.SPLASH, true, Items.SPLASH_POTION),
        ENTITY_SHAKE(GameEvent.ENTITY_SHAKE, false, Items.BLACK_CARPET, Items.BLUE_CARPET, Items.CYAN_CARPET, Items.BROWN_CARPET, Items.GRAY_CARPET, Items.GREEN_CARPET, Items.LIGHT_BLUE_CARPET, Items.LIGHT_GRAY_CARPET, Items.LIME_CARPET, Items.MAGENTA_CARPET, Items.MOSS_CARPET, Items.ORANGE_CARPET, Items.PINK_CARPET, Items.PURPLE_CARPET, Items.RED_CARPET, Items.WHITE_CARPET, Items.YELLOW_CARPET),
        BLOCK_CHANGE(GameEvent.BLOCK_CHANGE, false, Items.OBSERVER),
        NOTE_BLOCK_PLAY(GameEvent.NOTE_BLOCK_PLAY, false, Items.NOTE_BLOCK),
        PROJECTILE_SHOOT(GameEvent.PROJECTILE_SHOOT, true, Items.BOW),
        DRINK(GameEvent.DRINK, true, Items.GLASS_BOTTLE),
        PRIME_FUSE(GameEvent.PRIME_FUSE, false, Items.FLINT_AND_STEEL),
        PROJECTILE_LAND(GameEvent.PROJECTILE_LAND, true, Items.ARROW),
        EAT(GameEvent.EAT, true, Items.APPLE),
        ENTITY_INTERACT(GameEvent.ENTITY_INTERACT, true, Items.SADDLE),
        ENTITY_DAMAGE(GameEvent.ENTITY_DAMAGE, true, Items.WOODEN_SWORD, Items.STONE_SWORD, Items.GOLDEN_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD),
        EQUIP(GameEvent.EQUIP, true, Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE),
        SHEAR(GameEvent.SHEAR, true, Items.SHEARS),
        ENTITY_ROAR(GameEvent.ENTITY_ROAR, false, Items.DRAGON_EGG),
        BLOCK_CLOSE(GameEvent.BLOCK_CLOSE, false, Items.DARK_OAK_TRAPDOOR, Items.ACACIA_TRAPDOOR, Items.BIRCH_TRAPDOOR, Items.CRIMSON_TRAPDOOR, Items.IRON_TRAPDOOR, Items.JUNGLE_TRAPDOOR, Items.MANGROVE_TRAPDOOR, Items.OAK_TRAPDOOR, Items.SPRUCE_TRAPDOOR, Items.WARPED_TRAPDOOR),
        BLOCK_DEACTIVATE(GameEvent.BLOCK_DEACTIVATE, true, Items.REDSTONE_LAMP),
        BLOCK_DETACH(GameEvent.BLOCK_DETACH, true, Items.STRING),
        DISPENSE_FAIL(GameEvent.DISPENSE_FAIL, false, Items.DISPENSER),
        BLOCK_OPEN(GameEvent.BLOCK_OPEN, true, Items.DARK_OAK_DOOR, Items.ACACIA_DOOR, Items.BIRCH_DOOR, Items.CRIMSON_DOOR, Items.IRON_DOOR, Items.JUNGLE_DOOR, Items.MANGROVE_DOOR, Items.OAK_DOOR, Items.SPRUCE_DOOR, Items.WARPED_DOOR),
        BLOCK_ACTIVATE(GameEvent.BLOCK_ACTIVATE, false, Items.POLISHED_BLACKSTONE_PRESSURE_PLATE, Items.ACACIA_PRESSURE_PLATE, Items.BIRCH_PRESSURE_PLATE, Items.CRIMSON_PRESSURE_PLATE, Items.DARK_OAK_PRESSURE_PLATE, Items.HEAVY_WEIGHTED_PRESSURE_PLATE, Items.JUNGLE_PRESSURE_PLATE, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.MANGROVE_PRESSURE_PLATE, Items.OAK_PRESSURE_PLATE, Items.SPRUCE_PRESSURE_PLATE, Items.STONE_PRESSURE_PLATE, Items.WARPED_PRESSURE_PLATE),
        BLOCK_ATTACH(GameEvent.BLOCK_ATTACH, true, Items.TRIPWIRE_HOOK),
        ENTITY_PLACE(GameEvent.ENTITY_PLACE, true, Items.EGG),
        BLOCK_PLACE(GameEvent.BLOCK_PLACE, false, Items.STONE),
        FLUID_PLACE(GameEvent.FLUID_PLACE, true, Items.LAVA_BUCKET, Items.WATER_BUCKET, Items.AXOLOTL_BUCKET, Items.COD_BUCKET, Items.PUFFERFISH_BUCKET, Items.SALMON_BUCKET, Items.TADPOLE_BUCKET, Items.TROPICAL_FISH_BUCKET),
        ENTITY_DIE(GameEvent.ENTITY_DIE, true, Items.BONE),
        BLOCK_DESTROY(GameEvent.BLOCK_DESTROY, true, Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.GOLDEN_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE),
        FLUID_PICKUP(GameEvent.FLUID_PICKUP, true, Items.BUCKET),
        ITEM_INTERACT_FINISH(GameEvent.ITEM_INTERACT_FINISH, true, Items.ITEM_FRAME),
        CONTAINER_CLOSE(GameEvent.CONTAINER_CLOSE, false, Items.BARREL),
        PISTON_CONTRACT(GameEvent.PISTON_CONTRACT, false, Items.STICKY_PISTON),
        PISTON_EXTEND(GameEvent.PISTON_EXTEND, false, Items.PISTON),
        CONTAINER_OPEN(GameEvent.CONTAINER_OPEN, false, Items.CHEST),
        ITEM_INTERACT_START(GameEvent.ITEM_INTERACT_START, true, Items.GLOW_ITEM_FRAME),
        EXPLODE(GameEvent.EXPLODE, true, Items.TNT),
        LIGHTNING_STRIKE(GameEvent.LIGHTNING_STRIKE, false, Items.LIGHTNING_ROD),
        INSTRUMENT_PLAY(GameEvent.INSTRUMENT_PLAY, true, Items.GOAT_HORN);

        private final List<Item> configureItems;
        @Nullable
        private final GameEvent gameEvent;
        private final boolean renderFlat;
        private final int rotateDegree;

        public List<Item> getConfigureItems() {
            return this.configureItems;
        }

        public boolean shouldRenderFlat() {
            return renderFlat;
        }
        
        BetterVibration(GameEvent gameEvent, boolean renderFlat, Item... configureItems) {
            this(gameEvent, renderFlat, 180, configureItems); // purple asked me to make items point north
        }
        BetterVibration(GameEvent gameEvent, boolean renderFlat, int rotateDegree, Item... configureItems) {
            this(gameEvent, renderFlat, rotateDegree, Arrays.stream(configureItems).toList());
        }
        BetterVibration(@Nullable GameEvent gameEvent, boolean renderFlat, int rotateDegree, List<Item> configureItems) {
            this.gameEvent = gameEvent;
            this.configureItems = configureItems;
            this.renderFlat = renderFlat;
            this.rotateDegree = rotateDegree;
        }

        @Override
        public String asString() {
            if(this.gameEvent == null) {
                return "none";
            }

            return this.gameEvent.getId();
        }

        public int getRotateDegree() {
            return rotateDegree;
        }
    }
}
