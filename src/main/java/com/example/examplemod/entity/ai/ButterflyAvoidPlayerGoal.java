package com.example.examplemod.entity.ai;

import com.example.examplemod.entity.Butterfly;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.player.Player;

/** Keeps ordinary players at a distance while allowing peaceful observation in flower forests. */
public final class ButterflyAvoidPlayerGoal extends AvoidEntityGoal<Player> {
    private static final float AVOID_DISTANCE = 4.0F;
    private static final double ESCAPE_SPEED = 2.0D;
    private static final double CLOSE_ESCAPE_SPEED = 2.4D;

    private final Butterfly butterfly;

    public ButterflyAvoidPlayerGoal(Butterfly butterfly) {
        super(
                butterfly,
                Player.class,
                AVOID_DISTANCE,
                ESCAPE_SPEED,
                CLOSE_ESCAPE_SPEED,
                Butterfly.SHOULD_AVOID
        );
        this.butterfly = butterfly;
    }

    @Override
    public boolean canContinueToUse() {
        return this.toAvoid != null
                && Butterfly.SHOULD_AVOID.test(this.toAvoid)
                && super.canContinueToUse();
    }

    @Override
    public void stop() {
        super.stop();
        this.butterfly.getNavigation().stop();
    }
}
