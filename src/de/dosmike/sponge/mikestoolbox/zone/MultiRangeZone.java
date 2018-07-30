package de.dosmike.sponge.mikestoolbox.zone;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.api.world.teleport.TeleportHelperFilter;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.reflect.TypeToken;

import de.dosmike.sponge.mikestoolbox.tracer.BoxTracer;
import de.dosmike.sponge.mikestoolbox.zone.BoxZones.EventManipulator;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class MultiRangeZone implements Zone {
	
	TeleportHelperFilter oobfilter = new TeleportHelperFilter() {

		@Override
		public String getId() {
			return "mikestoolbox:oobtpfilter";
		}

		@Override
		public String getName() {
			return "Out of Bounds Filter";
		}

		@Override
		public boolean isSafeFloorMaterial(BlockState blockState) {
			return TeleportHelperFilters.DEFAULT.isSafeFloorMaterial(blockState);
		}

		@Override
		public boolean isSafeBodyMaterial(BlockState blockState) {
			return TeleportHelperFilters.DEFAULT.isSafeBodyMaterial(blockState);
		}
		
		@Override
		public Tristate isValidLocation(World world, Vector3i position) {
			if (isInside(new Location<World>(world, position)))
				return Tristate.FALSE;
			return TeleportHelperFilter.super.isValidLocation(world, position);
		}

	};
	
	Set<String> permission; 
	UUID id;
	Set<Range> ranges;
	int priority;
	PluginContainer plugin;
	String name = null;
	@Override
	public Optional<String> getName() {
		return Optional.ofNullable(name);
	}
	@Override
	public void setName(String newName) {
		name = newName;
	}
	
	private MultiRangeZone(PluginContainer plugin) {
		permission = new HashSet<>();
		ranges = new HashSet<>();
		this.plugin = plugin;
	}
	@Override
	public PluginContainer getPlugin() {
		return plugin;
	}

	private Map<Class<?>, Collection<EventManipulator<?>>> manipulators = new HashMap<>();
	@SuppressWarnings("unchecked")
	public <E extends Event> Collection<EventManipulator<E>> getEventManipulators(Class<E> event) {
		List<EventManipulator<E>> collected = new LinkedList<>();
		manipulators.entrySet().stream()
				.filter(e->e.getKey().isAssignableFrom(event))
				.map(Map.Entry::getValue)
				.forEach(c->collected.addAll((Collection<? extends EventManipulator<E>>) c));
		return collected;
	}
	
	@Override
	public Optional<Location<World>> eject(Entity e) throws IllegalArgumentException {
		if (!e.getBoundingBox().isPresent()) throw new IllegalArgumentException("Entity "+e.getTranslation().get()+" has no bounding box");
		Location<World> tN = eject(e.getLocation(), e.getBoundingBox().get(), Direction.NORTH.asOffset());
		Location<World> tE = eject(e.getLocation(), e.getBoundingBox().get(), Direction.EAST.asOffset());
		Location<World> tS = eject(e.getLocation(), e.getBoundingBox().get(), Direction.SOUTH.asOffset());
		Location<World> tW = eject(e.getLocation(), e.getBoundingBox().get(), Direction.WEST.asOffset());
		
		Optional<Location<World>> otN = Sponge.getTeleportHelper().getSafeLocation(tN, 5, 4, 2, oobfilter);
		Optional<Location<World>> otE = Sponge.getTeleportHelper().getSafeLocation(tE, 5, 4, 2, oobfilter);
		Optional<Location<World>> otS = Sponge.getTeleportHelper().getSafeLocation(tS, 5, 4, 2, oobfilter);
		Optional<Location<World>> otW = Sponge.getTeleportHelper().getSafeLocation(tW, 5, 4, 2, oobfilter);
		
		Location<World> theChoosenOne = null;
		double theChoosenDist=-1;
		
		if (otN.isPresent()) {
			theChoosenOne = otN.get();
			theChoosenDist = theChoosenOne.getPosition().distanceSquared(e.getLocation().getPosition());
		}
		if (otE.isPresent()) {
			double dist = otE.get().getPosition().distanceSquared(e.getLocation().getPosition());
			if (theChoosenOne==null || dist<theChoosenDist) {
				theChoosenOne = otN.get();
				theChoosenDist = dist;
			}
		}
		if (otS.isPresent()) {
			double dist = otS.get().getPosition().distanceSquared(e.getLocation().getPosition());
			if (theChoosenOne==null || dist<theChoosenDist) {
				theChoosenOne = otS.get();
				theChoosenDist = dist;
			}
		}
		if (otW.isPresent()) {
			double dist = otW.get().getPosition().distanceSquared(e.getLocation().getPosition());
			if (theChoosenOne==null || dist<theChoosenDist) {
				theChoosenOne = otW.get();
				theChoosenDist = dist;
			}
		}
		
		return (theChoosenOne == null || theChoosenDist<=0 )? Optional.empty() : Optional.of(theChoosenOne);
	}
	
	@Override
	public Optional<Location<World>> eject(Entity e, Vector3d direction) {
		if (!e.getBoundingBox().isPresent()) throw new IllegalArgumentException("Entity "+e.getTranslation().get()+" has no bounding box");
		Location<World> target = eject(e.getLocation(), e.getBoundingBox().get(), Direction.NORTH.asOffset());
		return Sponge.getTeleportHelper().getSafeLocation(target, 3, 9, 2, oobfilter); //Default values
	}
	
	private Location<World> eject(Location<World> ebase, AABB entity, Vector3d direction) {
		List<Range> inside = new LinkedList<>();
		for (Range r : ranges)
			if (r.intersects(entity))
				inside.add(r);
		
		while (!inside.isEmpty()) {
			Range first = inside.get(0);
			double pushX = direction.getX()<0
				? first.getMin().getX()-entity.getMax().getX()
				: first.getMax().getX()-entity.getMin().getX();
			double pushY = direction.getY()<0
					? first.getMin().getY()-entity.getMax().getY()
					: first.getMax().getY()-entity.getMin().getY();
			double pushZ = direction.getZ()<0
					? first.getMin().getZ()-entity.getMax().getZ()
					: first.getMax().getZ()-entity.getMin().getZ();
					
			//determ the min scalar to multiply direction with, that allows us to eject entity
			pushX = pushX / direction.getX(); 
			pushY = pushY / direction.getY();
			pushZ = pushZ / direction.getZ();
			double s = pushX<pushY?pushX:pushY; s = s<pushZ?s:pushZ;
			
			//move stuff around
			Vector3d move = direction.mul(s);
			ebase = ebase.add(move);
			entity = entity.offset(move);
			
			//refresh ranges we are inside now
			inside.clear();
			for (Range r : ranges)
				if (r.intersects(entity))
					inside.add(r);
		}
		return ebase;
	}
	
	@Override
	public UUID getID() {
		return id;
	}
	@Override
	public boolean hasPermission(Living e) {
		if (e instanceof Player) {
			Player p = (Player)e;
			for (String perm : permission)
				if (!p.hasPermission(perm)) return false;
		}
		return true;
	}
	@Override
	public void addPermission(String newName) {
		permission.add(newName);
	}
	@Override
	public void removePermission(String newName) {
		permission.remove(newName);
	}
	@Override
	public String[] listPermission(String newName) {
		return permission.toArray(new String[0]);
	}
	@Override
	public boolean isInside(Entity e) {
		for (Range r : ranges)
			if (r.isInside(e)) return true;
		return false;
	}
	@Override
	public boolean isInside(Location<?> loc) {
		if (!ranges.iterator().next().getExtent().equals(loc.getExtent())) return false;
		for (Range r : ranges)
			if (r.contains(loc.getPosition())) return true;
		return false;
	}
	@Override
	public int getPriority() {
		return priority;
	}
	@Override
	public void setPriority(int priority) {
		this.priority = priority; 
	}
	@Override
	public void trace(Viewer v, Entity highlight, BoxTracer inactive, BoxTracer active, BoxTracer targetRange) {
		if (!isInside(highlight)) {
			for (Range r : ranges) 
				r.trace(v, inactive);
		} else { 
			for (Range r : ranges) 
				r.trace(v, r.isInside(highlight)?targetRange:active);
		}
	}
	
	public static class Builder {
		MultiRangeZone result;
		Extent extent;
		
		private Builder(PluginContainer plugin, Extent extent) {
			result = new MultiRangeZone(plugin);
			result.id = UUID.randomUUID();
			result.priority=0;
			this.extent = extent;
		}
		public Builder addPermission(String permission) {
			result.permission.add(permission);
			return this;
		}
		public Builder addRange(Range range) {
			assert range.context.equals(extent.getUniqueId()): "Range in different Extent";
			result.ranges.add(range);
			return this;
		}
		public Builder addRange(Vector3i a, Vector3i b) {
			result.ranges.add(new Range(extent, a,b));
			return this;
		}
		/** this method is only required for deserialization, a id should already be set */
		public Builder setUUID(UUID id) {
			result.id = id;
			return this;
		}
		public Builder setName(String name) {
			result.name = name;
			return this;
		}
		public Builder setPriority(int priority) {
			result.priority = priority;
			return this;
		}
		public <E extends Event> Builder addManipulator(Class<E> clz, EventManipulator<E> manipulator) {
			Collection<EventManipulator<?>> manips;
			if (result.manipulators.containsKey(clz)) {
				manips = result.manipulators.get(clz);
			} else {
				manips = new HashSet<>();
			}
			manips.add(manipulator);
			result.manipulators.put(clz, manips);
			return (Builder) this;
		}
		public <E extends Event> Builder addManipulators(Class<E> clz, Collection<EventManipulator<E>> list) {
			Collection<EventManipulator<?>> manips;
			if (result.manipulators.containsKey(clz)) {
				manips = result.manipulators.get(clz);
			} else {
				manips = new HashSet<>();
			}
			manips.addAll(list);
			result.manipulators.put(clz, manips);
			return (Builder) this;
		}
		public MultiRangeZone build() {
			return result;
		}
	}
	public static Builder builder(PluginContainer plugin, Extent extent) {
		return new Builder(plugin, extent);
	}
	
	public static class Serializer implements TypeSerializer<MultiRangeZone> {

		@Override
		public MultiRangeZone deserialize(TypeToken<?> arg0, ConfigurationNode arg1) throws ObjectMappingException {
			PluginContainer pc = Sponge.getPluginManager().getPlugin(arg1.getNode("Plugin").getString()).orElseThrow(()->new ObjectMappingException("Plugin not found"));
			MultiRangeZone ret = new MultiRangeZone(pc);
			ret.id = UUID.fromString(arg1.getNode("UUID").getString());
			ret.name = arg1.getNode("Name").getString(null);
			{
				List<String> p = arg1.getNode("Permission").getList(TypeToken.of(String.class));
				ret.permission = new HashSet<>();
				ret.permission.addAll(p);
			}
			ret.priority = arg1.getNode("Priority").getInt(0);
			{
				List<Range> r = arg1.getNode("Ranges").getList(TypeToken.of(Range.class));
				ret.ranges = new HashSet<>();
				ret.ranges.addAll(r);
			}
			return ret;
		}
		
		@Override
		@SuppressWarnings("serial")
		public void serialize(TypeToken<?> arg0, MultiRangeZone arg1, ConfigurationNode arg2)
				throws ObjectMappingException {
			
			arg2.getNode("Plugin").setValue(arg1.plugin.getId());
			arg2.getNode("UUID").setValue(arg1.id.toString());
			if (arg1.name != null) arg2.getNode("Name").setValue(arg1.name);
			{
				List<String> l = new LinkedList<>();
				l.addAll(arg1.permission);
				arg2.getNode("Permission").setValue(new TypeToken<List<String>>(){}, l);
			}
			arg2.getNode("Priority").setValue(arg1.priority);
			{
				List<Range> r = new LinkedList<>();
				r.addAll(arg1.ranges);
				arg2.getNode("Ranges").setValue(new TypeToken<List<Range>>(){}, r);
			}
		}
		
	}

}
