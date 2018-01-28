package de.dosmike.sponge.mikestoolbox.zone;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import de.dosmike.sponge.mikestoolbox.event.BoxZoneEvent;
import de.dosmike.sponge.mikestoolbox.service.ZoneService;
import de.dosmike.sponge.mikestoolbox.zone.BoxZones.EventManipulator;

public class ZoneServiceProvider implements ZoneService {

	Set<Zone> zones = new HashSet<>();
	Map<UUID, Set<UUID>> status = new HashMap<>(); //map set of players in zone 
	
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
	public void removeZones(Class<? extends Zone> clz) {
		zones.removeIf(zone->{
			boolean del=clz.isAssignableFrom(zone.getClass());
			if (del) status.remove(zone.getID());
			return del;
		});
	}

	@Override
	public void removeZones(PluginContainer plugin) {
		zones.removeIf(zone->{
			boolean del=zone.getPlugin().getId().equals(plugin.getId());
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
	public Collection<Zone> getAllZones() {
		List<Zone> result = zones.stream()
				.collect(Collectors.toList());
		return result;
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
	
	@Override
	public Collection<Zone> getZonesByPlugin(PluginContainer plugin) {
		List<Zone> result = zones.stream()
				.filter(zone->zone.getPlugin().getId().equals(plugin.getId()))
				.collect(Collectors.toList());
		Collections.sort(result, (a,b)->{
			return Integer.compare(a.getPriority(), b.getPriority());
		});
		return result;
	}
	
	@Override
	public void notifyEntityDestroyed(Entity e) {
		List<BoxZoneEvent> eventStack = new LinkedList<>();
		for (Zone zone : zones) {
			if (!status.containsKey(zone.getID())) continue;
			Set<UUID> contained = status.get(zone.getID());
			if (contained.remove(e.getUniqueId())) {
				eventStack.add(new BoxZoneEvent.Post(e, BoxZoneEvent.Type.LEAVE, zone));
				status.put(zone.getID(), contained);
			}
		};
		for (BoxZoneEvent event : eventStack) {
			Sponge.getEventManager().post(event);
		}
	}
	
	@Override
	public void notifyEntityMoved(MoveEntityEvent event) {
//		Collection<Zone> inside = getZonesFor(event.getTargetEntity());
		Map<UUID, Zone> inside = new HashMap<>();
		getZonesFor(event.getTargetEntity()).forEach(zone->inside.put(zone.getID(), zone));
		Set<UUID> previous = new HashSet<>(); //zone ids 
		Set<Zone> in = new HashSet<>();
		Set<Zone> out = new HashSet<>();
		
		UUID ent = event.getTargetEntity().getUniqueId();
		for (Entry<UUID, Set<UUID>> e : status.entrySet()) {
			if (e.getValue().contains(ent)) previous.add(e.getKey());
		}
		
		inside.forEach((zid, zone)->{if (!previous.contains(zid)) in.add(zone);});
		out = previous.stream().filter(zid->!inside.containsKey(zid)).map(zid->getZone(zid).get()).collect(Collectors.toSet());
		
		boolean cancelled = event.isCancelled();
		for (Zone zone : in) {
			BoxZoneEvent.Pre boxevent = new BoxZoneEvent.Pre(event.getTargetEntity(), BoxZoneEvent.Type.ENTER, zone, cancelled);
			Sponge.getEventManager().post(boxevent);
			if (boxevent.isCancelled() && !cancelled) { cancelled = true; event.setCancelled(true); }
		};
		for (Zone zone : out) {
			BoxZoneEvent.Pre boxevent = new BoxZoneEvent.Pre(event.getTargetEntity(), BoxZoneEvent.Type.LEAVE, zone, cancelled);
			Sponge.getEventManager().post(boxevent);
			if (boxevent.isCancelled() && !cancelled) { cancelled = true; event.setCancelled(true); }
		};
		
		if (!cancelled) {
			for (Zone zone : in) {
				BoxZoneEvent.Post boxevent = new BoxZoneEvent.Post(event.getTargetEntity(), BoxZoneEvent.Type.ENTER, zone);
				Sponge.getEventManager().post(boxevent);
			};
			for (Zone zone : out) {
				BoxZoneEvent.Post boxevent = new BoxZoneEvent.Post(event.getTargetEntity(), BoxZoneEvent.Type.LEAVE, zone);
				Sponge.getEventManager().post(boxevent);
			};
			
			//update status
			for (Zone z : in) {
				Set<UUID> contained;
				if (!status.containsKey(z.getID()))
					contained = new HashSet<>();
				else
					contained = status.get(z.getID());
				contained.add(ent);
				status.put(z.getID(), contained);
			}
			for (Zone z : out) {
				if (status.containsKey(z.getID())) {
					Set<UUID> contained = status.get(z.getID());
					contained.remove(ent);
					if (contained.isEmpty()) 
						status.remove(z.getID());
					else
						status.put(z.getID(), contained);
				}
			}
		}
	}
	
	@Override
	public void notifyEventManipulators(Event event, Entity trigger) {
		UUID ent = trigger.getUniqueId();
		for (Zone zone : zones) if (status.containsKey(zone.getID()) && status.get(zone.getID()).contains(ent))
			for (EventManipulator<? extends Event> manip : zone.getEventManipulators(event.getClass()))
				anyZoneHandler(event, manip, zone);
	}
	@SuppressWarnings("unchecked")
	private static <E extends Event> void anyZoneHandler(Event e, EventManipulator<E> m, Zone z) {
		m.manipulate((E) e, z);
	}
}
