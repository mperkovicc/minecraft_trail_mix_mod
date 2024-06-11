package net.cinco.trailmixmod.block.custom;

import net.minecraft.world.phys.Vec3;

public class GlowstoneTrailBlock extends TrailBlock {
    public GlowstoneTrailBlock(Properties p_49795_) {
        super(p_49795_);
        connectTo.add(GlowstoneTrailBlock.class);
        COLOR = new Vec3(1.0d, 0.74d, 0.37d);
    }
}
