package de.dosmike.sponge.mikestoolbox.living;

import com.flowpowered.math.vector.Vector3d;
import de.dosmike.sponge.mikestoolbox.tracer.BoxTracer;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;

/** the Player ToolBox for various interactions with the player */
public class BoxPlayer extends BoxLiving {
	
	public static Vector3d getHeadDirection(Player player) {
		double pitch = Math.toRadians(player.getHeadRotation().getX()), yaw = Math.toRadians(player.getHeadRotation().getY());
		double x,y,z;
		
		x = -Math.sin(yaw)*Math.cos(pitch);
		y = -Math.sin(pitch);
		z = Math.cos(yaw)*Math.cos(pitch);
		
		return new Vector3d(x,y,z).normalize();
	}
	
	public static void shootEntity(Player player, Entity entity, float speed) {
		entity.setLocation(player.getLocation().add(0.0, 1.62, 0.0));
		entity.setVelocity(getHeadDirection(player).mul(speed));
		player.getWorld().spawnEntity(entity/*, Cause.builder().from(BoxLoader.blame()).named(NamedCause.source(player)).build()*/);
	}
	
	/** Convenience function for BoxTracer.drawTrace(ParticleEffect.Builder..., Player, Player.getPosition, position) */
	public static void tracePosition(Player player, org.spongepowered.api.util.Color color, Vector3d position) {
		BoxTracer.drawTrace(ParticleEffect.builder().type(ParticleTypes.REDSTONE_DUST).option(ParticleOptions.COLOR, color).build(), player, player.getLocation().getPosition(), position);
	}
	/** Convenience function for BoxTracer.drawTrace(ParticleEffect.Builder..., Player, Player.getPosition, position) */
	public static void tracePosition(Player player, java.awt.Color color, Vector3d position) {
		tracePosition(player, org.spongepowered.api.util.Color.of(color), position);
	}
	/** Convenience function for BoxTracer.drawTrace(ParticleEffect.Builder..., Player, Player.getPosition, position) */
	public static void tracePosition(Player player, ParticleEffect efx, Vector3d position) {
		BoxTracer.drawTrace(efx, player, player.getLocation().getPosition(), position);
	}
}
