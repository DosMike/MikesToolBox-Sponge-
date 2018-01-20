package de.dosmike.sponge.mikestoolbox.service;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import de.dosmike.sponge.mikestoolbox.zone.Zone;

public interface ZoneService {
	Optional<Zone> getZone(UUID zoneid);
	
	void addZone(Zone z);
	void removeZone(Class<? extends Zone> clz);
	void removeZone(UUID zoneid);
	
	/** returns a collection of zones sorted by priority */
	Collection<Zone> getZonesFor(Entity p);
	/** returns a collection of zones sorted by priority */
	Collection<Zone> getZonesAt(Location<World> location);
}
