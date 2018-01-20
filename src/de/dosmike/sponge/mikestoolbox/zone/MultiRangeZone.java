package de.dosmike.sponge.mikestoolbox.zone;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
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

import de.dosmike.sponge.mikestoolbox.tracer.BoxTracer;

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
	
	private MultiRangeZone() {
		permission = new HashSet<>();
		ranges = new HashSet<>();
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
	public void trace(Viewer v, Entity highlight, BoxTracer inactive, BoxTracer active, BoxTracer targetRange) {
		if (!isInside(highlight))
			for (Range r : ranges) 
				r.trace(v, inactive);
		else 
			for (Range r : ranges) 
				r.trace(v, r.isInside(highlight)?targetRange:active);
	}
	
	public static class Builder {
		MultiRangeZone result = new MultiRangeZone();
		Extent extent;
		
		private Builder(Extent extent) {
			result.id = UUID.randomUUID();
			result.priority=0;
		}
		public Builder addPermission(String permission) {
			result.permission.add(permission);
			return this;
		}
		public Builder addRange(Range range) {
			assert range.context.equals(extent): "Range in different Extent";
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
		public Builder setPriority(int priority) {
			result.priority = priority;
			return this;
		}
		public MultiRangeZone build() {
			return result;
		}
	}
	public static Builder builder(Extent extent) {
		return new Builder(extent);
	}
}
