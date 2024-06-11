package net.cinco.trailmixmod.block.custom;

import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.phys.Vec3;

public class GunpowderTrailBlock extends TrailBlock {
    public GunpowderTrailBlock(Properties p_49795_) {
        super(p_49795_);
        connectTo.add(GunpowderTrailBlock.class);
        connectTo.add(FireBlock.class);
        COLOR = new Vec3(0.45d, 0.45d, 0.45d);
    }
}
