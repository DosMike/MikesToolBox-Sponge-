package de.dosmike.sponge.mikestoolbox.service;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import de.dosmike.sponge.mikestoolbox.event.BoxZoneEvent;
import de.dosmike.sponge.mikestoolbox.zone.BoxZones.EventManipulator;
import de.dosmike.sponge.mikestoolbox.zone.Zone;

public interface ZoneService {
	Optional<Zone> getZone(UUID zoneid);
	
	/** add a new zone to the handler */
	void addZone(Zone z);
	/** remove all zones for this zone class<br>
	 * Zones should be removed where zone is instance of clz */
	void removeZones(Class<? extends Zone> clz);
	/** remove a specific zone by it's UUID */
	void removeZone(UUID zoneid);
	/** all zones from this plugin will be removed. usefull for reloads */
	void removeZones(PluginContainer plugin);
	
	/** Return all currently registered Zones */
	Collection<Zone> getAllZones();
	/** Collects all zones a entity is currently inside
	 * @return a collection of zones sorted by priority */
	Collection<Zone> getZonesFor(Entity p);
	/** Collects all zones that contain a certain location
	 * @return a collection of zones sorted by priority */
	Collection<Zone> getZonesAt(Location<World> location);
	/** Collects all zones added by the specified plugin
	 * @return a collection of zones sorted by priority */
	Collection<Zone> getZonesByPlugin(PluginContainer plugin);
	
	/** this method gets called on the service from a {@link MoveEntityEvent}-Listener
	 * and should be used to update a zone cache for a certain entity<br>
	 * This method receives the whole event, to allow it to perform permission checks 
	 * and cancel it if necessary<br>
	 * For each zone the entity entered or left it should post a {@link BoxZoneEvent} 
	 * on the event bus and check for cancellations through these events */
	void notifyEntityMoved(MoveEntityEvent event);
	
	/** this method gets called on the service from a {@link DestructEntityEvent}-Listener
	 * and should be used to update a zone cache for tracked entities */
	void notifyEntityDestroyed(Entity e);
	
	/** This notifier is supposed to call all event manipulators on all zones the
	 * entity is currently in. This should greatly simplify protections to a degree of adding
	 * a bunch of {@link EventManipulator}.
	 */
	void notifyEventManipulators(Event event, Entity trigger);
	
}
