package de.dosmike.sponge.mikestoolbox.zone;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

import de.dosmike.sponge.mikestoolbox.tracer.BoxTracer;
import de.dosmike.sponge.mikestoolbox.zone.BoxZones.EventManipulator;

public interface Zone extends Comparable<Zone>{
	/** this is for more advanced plugins, that require to manage overlapping zones.
	 * Usually higher values mean more important */
	int getPriority();
	@Override
	default public int compareTo(Zone other) {
		return Integer.compare(getPriority(), other.getPriority());
	}
	/** @return the {@link PluginContainer} for the plugin that created this zone */
	PluginContainer getPlugin();
	
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
	
	
	/** list of event manipulators, when an event is called the manipulator tries to execute<br>
	 * these are dynamic event listeners only active while the item is held.
	 * if your zone does not support this, you can always return a empty collection */
	public <E extends Event> Collection<EventManipulator<E>> getEventManipulators(Class<E> event);
}
