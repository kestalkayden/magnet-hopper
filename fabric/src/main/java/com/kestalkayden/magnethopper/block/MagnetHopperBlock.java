package com.kestalkayden.magnethopper.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MagnetHopperBlock extends BaseEntityBlock {

    // simpleCodec only supports 1-arg constructors; tier is set at registration time, so the
    // codec default of BASIC is fine — every actual placed block has its real tier from its block instance.
    public static final MapCodec<MagnetHopperBlock> CODEC = simpleCodec(props -> new MagnetHopperBlock(MagnetTier.BASIC, props));

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING_HOPPER;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    private static final VoxelShape INSIDE     = box(2, 11, 2, 14, 16, 14);
    private static final VoxelShape OUTER_CUT  = Shapes.join(Shapes.block(), INSIDE, BooleanOp.ONLY_FIRST);
    private static final VoxelShape DOWN_SPOUT = Shapes.or(
        box(6, 4, 6, 10, 11, 10),
        box(4, 0, 4, 12, 4, 12));
    private static final VoxelShape SHAPE = Shapes.or(OUTER_CUT, DOWN_SPOUT);

    private final MagnetTier tier;

    public MagnetHopperBlock(MagnetTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.DOWN)
            .setValue(ENABLED, true));
    }

    public MagnetTier getTier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace().getOpposite();
        if (facing == Direction.UP) facing = Direction.DOWN;
        boolean enabled = !context.getLevel().hasNeighborSignal(context.getClickedPos());
        return defaultBlockState().setValue(FACING, facing).setValue(ENABLED, enabled);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                    Orientation orientation, boolean isMoving) {
        boolean shouldEnable = !level.hasNeighborSignal(pos);
        if (shouldEnable != state.getValue(ENABLED)) {
            level.setBlock(pos, state.setValue(ENABLED, shouldEnable), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INSIDE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagnetHopperBlockEntity(pos, state);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MagnetHopperBlockEntity be) {
            // Spill main storage into the world (filter slots are not lootable — they're config, not contents).
            Containers.dropContents(level, pos, be);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MagnetHopperBlockEntity be) {
            player.openMenu(be);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof MagnetHopperBlockEntity mh) {
                mh.serverTick((net.minecraft.server.level.ServerLevel) lvl, pos, st);
            }
        };
    }
}
