package de.dosmike.sponge.mikestoolbox.zone;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import de.dosmike.sponge.mikestoolbox.service.ZoneService;

public class ZoneServiceProvider implements ZoneService {

	Set<Zone> zones = new HashSet<>();
	Map<UUID, Set<UUID>> status = new HashMap<>();
	
	@Override
	public Optional<Zone> getZone(UUID zoneid) {
		for (Zone z : zones) if (z.getID().equals(zoneid)) return Optional.of(z);
		return Optional.empty();
	}

	@Override
	public void addZone(Zone z) {
		zones.add(z);
	}

	@Override
	public void removeZone(Class<? extends Zone> clz) {
		zones.removeIf(zone->{
			boolean del=clz.isAssignableFrom(zone.getClass());
			if (del) status.remove(zone.getID());
			return del;
		});
	}

	@Override
	public void removeZone(UUID zoneid) {
		zones.removeIf(zone->zone.getID().equals(zoneid));
		status.remove(zoneid);
	}

	@Override
	public Collection<Zone> getZonesFor(Entity p) {
		List<Zone> result = zones.stream()
				.filter(zone->zone.isInside(p))
				.collect(Collectors.toList());
		Collections.sort(result, (a,b)->{
			return Integer.compare(a.getPriority(), b.getPriority());
		});
		return result;
	}

	@Override
	public Collection<Zone> getZonesAt(Location<World> location) {
		List<Zone> result = zones.stream()
				.filter(zone->zone.isInside(location))
				.collect(Collectors.toList());
		Collections.sort(result, (a,b)->{
			return Integer.compare(a.getPriority(), b.getPriority());
		});
		return result;
	}
	
}
