package de.dosmike.sponge.mikestoolbox.tracer;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleTypes;

/** keep in mind that a viewer can be a player as well as a world (or other viewer implementations) */
public class BoxTracer {
	ParticleEffect efx;
	public BoxTracer(ParticleEffect effect) {
		efx = effect;
	}
	public BoxTracer(org.spongepowered.api.util.Color color) {
		efx = ParticleEffect.builder().type(ParticleTypes.REDSTONE_DUST)
				.option(ParticleOptions.COLOR, color)
				.build();
	}
	public BoxTracer(java.awt.Color color) {
		efx = ParticleEffect.builder().type(ParticleTypes.REDSTONE_DUST)
				.option(ParticleOptions.COLOR, org.spongepowered.api.util.Color.of(color))
				.build();
	}
	
	public void drawTrace(Viewer w, Vector3d a, Vector3d b) {
		drawTrace(efx, w, a, b);
	}
	public void drawTrace(Viewer w, Vector3i a, Vector3i b) {
		drawTrace(efx, w, a.toDouble(), b.toDouble());
	}
	public static void drawTrace(java.awt.Color color, Viewer w, Vector3d a, Vector3d b) {
		drawTrace(ParticleEffect.builder().type(ParticleTypes.REDSTONE_DUST)
				.option(ParticleOptions.COLOR, org.spongepowered.api.util.Color.of(color))
				.build(), w, a, b);
	}
	public static void drawTrace(org.spongepowered.api.util.Color color, Viewer w, Vector3d a, Vector3d b) {
		drawTrace(ParticleEffect.builder().type(ParticleTypes.REDSTONE_DUST)
				.option(ParticleOptions.COLOR, color)
				.build(), w, a, b);
	}
	public static void drawTrace(ParticleEffect effect, Viewer w, Vector3i a, Vector3i b) {
		drawTrace(effect, w, a.toDouble(), b.toDouble());
	}
	
	public static void drawTrace(ParticleEffect effect, Viewer w, Vector3d a, Vector3d b) {
		//go from b to a
		if (a.distance(b)<0.2) return;
		Vector3d position = b;
		Vector3d direction = a.sub(b);
		direction = direction.div(direction.length()*5.0); //10 particles per block distance
//		Vector3d pa = a.sub(position); //target validation vector
		double distance = a.distanceSquared(position), predist;
//		while (a.distanceSquared(position)>0.02 /*a.dot(pa) >= 0*/) { //while we are moving towards the target (a)
		do {
			predist = distance;
			w.spawnParticles(effect, position); //spawn particle
			
			position = position.add(direction); //move towards target
//			pa = a.sub(position); //target validation vector
		} while ( (distance = a.distanceSquared(position)) <= predist );
	}
}
