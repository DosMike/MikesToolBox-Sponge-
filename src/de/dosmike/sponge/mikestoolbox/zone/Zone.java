package de.dosmike.sponge.mikestoolbox.zone;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

import de.dosmike.sponge.mikestoolbox.tracer.BoxTracer;

public interface Zone {
	/** this is for more advanced plugins, that require to manage overlapping zones.
	 * Usually higher values mean more important */
	int getPriority();
	
	/** check whether an entity is inside this zone */
	boolean isInside(Entity e);
	boolean isInside(Location<?> loc);
	
	/** get this zones unique ID */
	UUID getID();
	
	/** returns whether this living is allowed to enter the zone or not */
	boolean hasPermission(Living e);
	
	/** Returns a location for this entity outside this zone.
	 * The implementation should look for a save location outside the zone to teleport to. 
	 * May not work well with overlapping zones, depends on implementation */
	Optional<Location<World>> eject(Entity e);
	/** Tries to move a entity outside the range in the given direction
	 * See eject(Entity) for more. Will be faster than eject(Entity). */
	Optional<Location<World>> eject(Entity e, Vector3d direction);
	
	/** this method is meant to visually display a zone by tracing the outlines with <b>inactive</b>, 
	 * if the highlight entity is <i>outside</i> the zone; and <b>active</b> and <b>targetRange</b> if highlight is <i>inside<i> the zone.
	 * where the primary range uses <b>targetRange</b> and other ranges that are part of the active zone use <b>active</b>.*/
	void trace(Viewer v, Entity highlight, BoxTracer inactive, BoxTracer active, BoxTracer targetRange);
}
