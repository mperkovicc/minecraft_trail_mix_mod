package net.cinco.trailmixmod.block.custom;

import net.minecraft.world.phys.Vec3;

public class SugarTrailBlock extends TrailBlock {
    public SugarTrailBlock(Properties p_49795_) {
        super(p_49795_);
        connectTo.add(SugarTrailBlock.class);
        COLOR = new Vec3(1.0d, 1.0d, 1.0d);
    }
}
