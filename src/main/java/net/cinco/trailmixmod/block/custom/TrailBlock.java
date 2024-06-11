package net.cinco.trailmixmod.block.custom;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TrailBlock extends Block {
    private enum TrailSide implements StringRepresentable {
        NONE("none"),
        SIDE("side"),
        UP("up");
        private final String name;

        TrailSide(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
        public boolean isConnected() {
            return this != TrailSide.NONE;
        }
    }
    public static final EnumProperty<TrailSide> NORTH = EnumProperty.create("north", TrailSide.class);
    public static final EnumProperty<TrailSide> EAST = EnumProperty.create("east", TrailSide.class);
    public static final EnumProperty<TrailSide> SOUTH = EnumProperty.create("south", TrailSide.class);
    public static final EnumProperty<TrailSide> WEST = EnumProperty.create("west", TrailSide.class);
    public static final Map<Direction, EnumProperty<TrailSide>> PROPERTY_BY_DIRECTION = Maps.newEnumMap(
            ImmutableMap.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST)
    );
    protected ArrayList<Class<? extends Block>> connectTo = new ArrayList<>();

    protected static Vec3 COLOR;
    public static int getIntColor() {
        return Mth.color((float)COLOR.x(), (float)COLOR.y(), (float)COLOR.z());
    }
    private static final VoxelShape SHAPE_DOT = Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 13.0);
    private static final Map<Direction, VoxelShape> SHAPES_FLOOR = Maps.newEnumMap(
            ImmutableMap.of(
                    Direction.NORTH,
                    Block.box(3.0, 0.0, 0.0, 13.0, 1.0, 13.0),
                    Direction.SOUTH,
                    Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 16.0),
                    Direction.EAST,
                    Block.box(3.0, 0.0, 3.0, 16.0, 1.0, 13.0),
                    Direction.WEST,
                    Block.box(0.0, 0.0, 3.0, 13.0, 1.0, 13.0)
            )
    );
    private static final Map<Direction, VoxelShape> SHAPES_UP = Maps.newEnumMap(
            ImmutableMap.of(
                    Direction.NORTH,
                    Shapes.or(SHAPES_FLOOR.get(Direction.NORTH), Block.box(3.0, 0.0, 0.0, 13.0, 16.0, 1.0)),
                    Direction.SOUTH,
                    Shapes.or(SHAPES_FLOOR.get(Direction.SOUTH), Block.box(3.0, 0.0, 15.0, 13.0, 16.0, 16.0)),
                    Direction.EAST,
                    Shapes.or(SHAPES_FLOOR.get(Direction.EAST), Block.box(15.0, 0.0, 3.0, 16.0, 16.0, 13.0)),
                    Direction.WEST,
                    Shapes.or(SHAPES_FLOOR.get(Direction.WEST), Block.box(0.0, 0.0, 3.0, 1.0, 16.0, 13.0))
            )
    );
    private static final HashMap<BlockState, VoxelShape> SHAPES_CACHE = Maps.newHashMap();
    private final BlockState crossState;
    public TrailBlock(Properties p_49795_) {
        super(p_49795_);
        COLOR = new Vec3(0.0d, 0.0d, 0.0d);

        this.registerDefaultState(
            this.stateDefinition
                .any()
                    .setValue(NORTH, TrailSide.NONE)
                    .setValue(EAST, TrailSide.NONE)
                    .setValue(SOUTH, TrailSide.NONE)
                    .setValue(WEST, TrailSide.NONE)
        );
        this.crossState = this.defaultBlockState()
                .setValue(NORTH, TrailSide.SIDE)
                .setValue(EAST, TrailSide.SIDE)
                .setValue(SOUTH, TrailSide.SIDE)
                .setValue(WEST, TrailSide.SIDE);

        for (BlockState blockState: this.getStateDefinition().getPossibleStates()) {
            SHAPES_CACHE.put(blockState, this.calculateShape(blockState));
        }
    }
    private VoxelShape calculateShape(BlockState blockState) {
        VoxelShape voxelShape = SHAPE_DOT;
        Iterator<Direction> iter = Direction.Plane.HORIZONTAL.iterator();

        while (iter.hasNext()) {
            Direction direction = iter.next();
            TrailSide trailSide = blockState.getValue(PROPERTY_BY_DIRECTION.get(direction));
            if (trailSide == TrailSide.SIDE) voxelShape = Shapes.or(voxelShape, SHAPES_FLOOR.get(direction));
            if (trailSide == TrailSide.UP) voxelShape = Shapes.or(voxelShape, SHAPES_UP.get(direction));
        }

        return voxelShape;
    }
    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState pState, @NotNull BlockGetter pLevel,
                                           @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return SHAPES_CACHE.get(pState);
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.getConnectionState(pContext.getLevel(), this.defaultBlockState(), pContext.getClickedPos());
    }
    private BlockState getConnectionState(BlockGetter pLevel, BlockState pState, BlockPos pPos) {
        boolean flag = isDot(pState);
        pState = this.getMissingConnections(pLevel, this.defaultBlockState(), pPos);
        if (!flag || !isDot(pState)) {
            boolean flag1 = pState.getValue(NORTH).isConnected();
            boolean flag2 = pState.getValue(SOUTH).isConnected();
            boolean flag3 = pState.getValue(EAST).isConnected();
            boolean flag4 = pState.getValue(WEST).isConnected();
            boolean flag5 = !flag1 && !flag2;
            boolean flag6 = !flag3 && !flag4;
            if (!flag4 && flag5) {
                pState = pState.setValue(WEST, TrailSide.SIDE);
            }

            if (!flag3 && flag5) {
                pState = pState.setValue(EAST, TrailSide.SIDE);
            }

            if (!flag1 && flag6) {
                pState = pState.setValue(NORTH, TrailSide.SIDE);
            }

            if (!flag2 && flag6) {
                pState = pState.setValue(SOUTH, TrailSide.SIDE);
            }

        }
        return pState;
    }
    private BlockState getMissingConnections(BlockGetter pLevel, BlockState pState, BlockPos pPos) {
        boolean flag = !pLevel.getBlockState(pPos.above()).isRedstoneConductor(pLevel, pPos);
        Iterator<Direction> iter = Direction.Plane.HORIZONTAL.iterator();

        while (iter.hasNext()) {
            Direction direction = iter.next();
            if (!pState.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected()) {
                TrailSide trailSide = this.getConnectingSide(pLevel, pPos, direction, flag);
                pState = pState.setValue(PROPERTY_BY_DIRECTION.get(direction), trailSide);
            }
        }

        return pState;
    }
    private TrailSide getConnectingSide(BlockGetter pLevel, BlockPos pPos, Direction pDirection, boolean pNonNormalCubeAbove) {
        BlockPos blockpos = pPos.relative(pDirection);
        BlockState blockstate = pLevel.getBlockState(blockpos);
        if (pNonNormalCubeAbove) {
            boolean flag = blockstate.getBlock() instanceof TrapDoorBlock || this.canSurviveOn(pLevel, blockpos, blockstate);

            if (flag && connectTo.contains(pLevel.getBlockState(blockpos.above()).getBlock().getClass())) {
                if (blockstate.isFaceSturdy(pLevel, blockpos, pDirection.getOpposite())) {
                    return TrailSide.UP;
                }

                return TrailSide.SIDE;
            }
        }

        if (connectTo.contains(blockstate.getBlock().getClass())) {
            return TrailSide.SIDE;
        } else if (blockstate.isRedstoneConductor(pLevel, blockpos)) {
            return TrailSide.NONE;
        } else {
            BlockPos blockPosBelow = blockpos.below();
            return connectTo.contains(pLevel.getBlockState(blockPosBelow).getBlock().getClass()) ? TrailSide.SIDE : TrailSide.NONE;
        }
    }
    protected boolean canSurvive(@NotNull BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        BlockState blockstate = pLevel.getBlockState(blockpos);
        return this.canSurviveOn(pLevel, blockpos, blockstate);
    }
    private boolean canSurviveOn(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        return pState.isFaceSturdy(pLevel, pPos, Direction.UP);
    }
    private boolean isDot(BlockState pState) {
        return !pState.getValue(NORTH).isConnected()
                && !pState.getValue(EAST).isConnected()
                && !pState.getValue(SOUTH).isConnected()
                && !pState.getValue(WEST).isConnected();
    }
    private boolean isCross(BlockState pState) {
        return pState.getValue(NORTH).isConnected()
                && pState.getValue(EAST).isConnected()
                && pState.getValue(SOUTH).isConnected()
                && pState.getValue(WEST).isConnected();
    }
    @NotNull
    protected BlockState updateShape(@NotNull BlockState pState, @NotNull Direction pFacing,
                                     @NotNull BlockState pFacingState, @NotNull LevelAccessor pLevel,
                                     @NotNull BlockPos pCurrentPos, @NotNull BlockPos pFacingPos) {
        if (pFacing == Direction.DOWN) {
            return !this.canSurviveOn(pLevel, pFacingPos, pFacingState) ? Blocks.AIR.defaultBlockState() : pState;
        } else if (pFacing == Direction.UP) {
            return this.getConnectionState(pLevel, pState, pCurrentPos);
        } else {
            boolean flag = !pLevel.getBlockState(pCurrentPos.above()).isRedstoneConductor(pLevel, pCurrentPos);
            TrailSide trailSide = this.getConnectingSide(pLevel, pCurrentPos, pFacing, flag);
            return trailSide.isConnected() == pState.getValue(PROPERTY_BY_DIRECTION.get(pFacing)).isConnected()
                    && !isCross(pState)
                    ? pState.setValue(PROPERTY_BY_DIRECTION.get(pFacing), trailSide)
                    : this.getConnectionState(pLevel, this.crossState
                            .setValue(PROPERTY_BY_DIRECTION.get(pFacing), trailSide), pCurrentPos);
        }
    }
    protected void updateIndirectNeighbourShapes(@NotNull BlockState pState, @NotNull LevelAccessor pLevel,
                                                 @NotNull BlockPos pPos, int pFlags, int pRecursionLeft) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        Iterator<Direction> iter = Direction.Plane.HORIZONTAL.iterator();

        while(iter.hasNext()) {
            Direction direction = iter.next();
            TrailSide trailSide = pState.getValue(PROPERTY_BY_DIRECTION.get(direction));
            if (trailSide.isConnected() && !pLevel.getBlockState(blockpos$mutableblockpos.setWithOffset(pPos, direction)).is(this)) {
                blockpos$mutableblockpos.move(Direction.DOWN);
                BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
                if (blockstate.is(this)) {
                    BlockPos blockpos = blockpos$mutableblockpos.relative(direction.getOpposite());
                    pLevel.neighborShapeChanged(direction.getOpposite(), pLevel.getBlockState(blockpos), blockpos$mutableblockpos, blockpos, pFlags, pRecursionLeft);
                }

                blockpos$mutableblockpos.setWithOffset(pPos, direction).move(Direction.UP);
                BlockState blockstate1 = pLevel.getBlockState(blockpos$mutableblockpos);
                if (blockstate1.is(this)) {
                    BlockPos blockpos1 = blockpos$mutableblockpos.relative(direction.getOpposite());
                    pLevel.neighborShapeChanged(direction.getOpposite(), pLevel.getBlockState(blockpos1), blockpos$mutableblockpos, blockpos1, pFlags, pRecursionLeft);
                }
            }
        }

    }
    private void checkCornerChangeAt(Level pLevel, BlockPos pPos) {
        if (pLevel.getBlockState(pPos).is(this)) {
            pLevel.updateNeighborsAt(pPos, this);
            Direction[] directions = Direction.values();

            for (Direction direction : directions) {
                pLevel.updateNeighborsAt(pPos.relative(direction), this);
            }
        }

    }
    private void updateNeighborsOfNeighboringWires(Level pLevel, BlockPos pPos) {
        Iterator<Direction> iter = Direction.Plane.HORIZONTAL.iterator();

        Direction direction1;
        while(iter.hasNext()) {
            direction1 = iter.next();
            this.checkCornerChangeAt(pLevel, pPos.relative(direction1));
        }

        iter = Direction.Plane.HORIZONTAL.iterator();

        while(iter.hasNext()) {
            direction1 = iter.next();
            BlockPos blockpos = pPos.relative(direction1);
            if (pLevel.getBlockState(blockpos).isRedstoneConductor(pLevel, blockpos)) {
                this.checkCornerChangeAt(pLevel, blockpos.above());
            } else {
                this.checkCornerChangeAt(pLevel, blockpos.below());
            }
        }

    }
    protected void onPlace(BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos,
                           BlockState pOldState, boolean pIsMoving) {
        if (!pOldState.is(pState.getBlock()) && !pLevel.isClientSide) {
            Iterator<Direction> iter = Direction.Plane.VERTICAL.iterator();

            while(iter.hasNext()) {
                Direction direction = iter.next();
                pLevel.updateNeighborsAt(pPos.relative(direction), this);
            }

            this.updateNeighborsOfNeighboringWires(pLevel, pPos);
        }

    }
    protected void onRemove(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos,
                            @NotNull BlockState pNewState, boolean pIsMoving) {
        if (!pIsMoving && !pState.is(pNewState.getBlock())) {
            super.onRemove(pState, pLevel, pPos, pNewState, false);
            if (!pLevel.isClientSide) {
                Direction[] directions = Direction.values();

                for (Direction direction : directions) {
                    pLevel.updateNeighborsAt(pPos.relative(direction), this);
                }

                this.updateNeighborsOfNeighboringWires(pLevel, pPos);
            }
        }

    }
    protected void neighborChanged(@NotNull BlockState pState, Level pLevel, @NotNull BlockPos pPos,
                                   @NotNull Block pBlock, @NotNull BlockPos pFromPos, boolean pIsMoving) {
        if (!pLevel.isClientSide) {
            if (!pState.canSurvive(pLevel, pPos)) {
                dropResources(pState, pLevel, pPos);
                pLevel.removeBlock(pPos, false);
            }
        }

    }
    @NotNull
    @Override
    protected BlockState rotate(@NotNull BlockState pState, Rotation pRotation) {
        return switch (pRotation) {
            case CLOCKWISE_180 -> pState.setValue(NORTH, pState.getValue(SOUTH)).setValue(EAST,
                    pState.getValue(WEST)).setValue(SOUTH, pState.getValue(NORTH)).setValue(WEST, pState.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> pState.setValue(NORTH, pState.getValue(EAST)).setValue(EAST,
                    pState.getValue(SOUTH)).setValue(SOUTH, pState.getValue(WEST)).setValue(WEST, pState.getValue(NORTH));
            case CLOCKWISE_90 -> pState.setValue(NORTH, pState.getValue(WEST)).setValue(EAST,
                    pState.getValue(NORTH)).setValue(SOUTH, pState.getValue(EAST)).setValue(WEST, pState.getValue(SOUTH));
            default -> pState;
        };
    }
    @NotNull
    protected BlockState mirror(@NotNull BlockState pState, Mirror pMirror) {
        return switch (pMirror) {
            case LEFT_RIGHT ->
                    pState.setValue(NORTH, pState.getValue(SOUTH)).setValue(SOUTH, pState.getValue(NORTH));
            case FRONT_BACK ->
                    pState.setValue(EAST, pState.getValue(WEST)).setValue(WEST, pState.getValue(EAST));
            default -> super.mirror(pState, pMirror);
        };
    }
    private void spawnParticlesAlongLine(Level pLevel, RandomSource pRandom, BlockPos pPos, Vec3 pParticleVec,
                                         Direction pXDirection, Direction pZDirection, float pMin, float pMax) {
        float f = pMax - pMin;
        if (!(pRandom.nextFloat() >= 0.2F * f)) {
            float f1 = 0.4375F;
            float f2 = pMin + f * pRandom.nextFloat();
            double d0 = 0.5 + (double)(f1 * (float)pXDirection.getStepX()) + (double)(f2 * (float)pZDirection.getStepX());
            double d1 = 0.5 + (double)(f1 * (float)pXDirection.getStepY()) + (double)(f2 * (float)pZDirection.getStepY());
            double d2 = 0.5 + (double)(f1 * (float)pXDirection.getStepZ()) + (double)(f2 * (float)pZDirection.getStepZ());
            pLevel.addParticle(new DustParticleOptions(pParticleVec.toVector3f(), 1.0F),
                    (double)pPos.getX() + d0, (double)pPos.getY() + d1, (double)pPos.getZ() + d2, 0.0, 0.0, 0.0);
        }

    }
    public void animateTick(@NotNull BlockState pState, @NotNull Level pLevel,
                            @NotNull BlockPos pPos, @NotNull RandomSource pRandom) {
        Iterator<Direction> iter = Direction.Plane.HORIZONTAL.iterator();

        while(iter.hasNext()) {
            Direction direction = iter.next();
            TrailSide trailSide = pState.getValue(PROPERTY_BY_DIRECTION.get(direction));
            switch (trailSide) {
                case UP:
                    this.spawnParticlesAlongLine(pLevel, pRandom, pPos, COLOR, direction, Direction.UP, -0.5F, 0.5F);
                case SIDE:
                    this.spawnParticlesAlongLine(pLevel, pRandom, pPos, COLOR, Direction.DOWN, direction, 0.0F, 0.5F);
                    break;
                case NONE:
                default:
                    this.spawnParticlesAlongLine(pLevel, pRandom, pPos, COLOR, Direction.DOWN, direction, 0.0F, 0.3F);
                }
            }

    }
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(NORTH, EAST, SOUTH, WEST);
    }
}
