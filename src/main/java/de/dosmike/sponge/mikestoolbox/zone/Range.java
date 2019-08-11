package de.dosmike.sponge.mikestoolbox.zone;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.reflect.TypeToken;
import de.dosmike.sponge.mikestoolbox.tracer.BoxTracer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;

import java.util.Optional;
import java.util.UUID;

public class Range extends AABB {
	UUID context=null;
	
	void setContextFromBuilder(Extent extent) {
		if (context == null) context = extent.getUniqueId();
	}
	
	/** by using the block position the high location will fall one block short. 
	 * A range with start position = end position would have a size of 0 if the high-block 
	 * is excluded. For this reason it will be included by default, making the size 
	 * 1x1x1 if a == b */
	public Range(Extent e, Vector3i a, Vector3i b) {
		super(a.min(b),a.max(b).add(1,1,1));
		context = e.getUniqueId();
	}
	/** by using the block position the high location will fall one block short. 
	 * A range with start position = end position would have a size of 0 if the high-block 
	 * is excluded. For this reason it will be included by default, making the size 
	 * 1x1x1 if a == b */
	public Range(Extent e, Vector3i a, Vector3i b, boolean excludeHigh) {
		super(a.min(b),excludeHigh?a.max(b):a.max(b).add(1,1,1));
		context = e.getUniqueId();
	}
	
	/** by using the block position the high location will fall one block short. 
	 * A range with start position = end position would have a size of 0 if the high-block 
	 * is excluded. For this reason it will be included by default, making the size 
	 * 1x1x1 if a == b<br>
	 * This constructor is recommended to Zone Builders */
	public Range(Vector3i a, Vector3i b) {
		super(a.min(b),a.max(b).add(1,1,1));
	}
	/** by using the block position the high location will fall one block short. 
	 * A range with start position = end position would have a size of 0 if the high-block 
	 * is excluded. For this reason it will be included by default, making the size 
	 * 1x1x1 if a == b<br>
	 * This constructor is recommended to Zone Builders */
	public Range(Vector3i a, Vector3i b, boolean excludeHigh) {
		super(a.min(b),excludeHigh?a.max(b):a.max(b).add(1,1,1));
	}
	public void trace(Viewer v, BoxTracer t) {
		Vector3d dim = getSize();
		Vector3d min = getMin(), max = getMax();
		t.drawTrace(v, min, min.add(dim.getX(), 0, 0));
		t.drawTrace(v, min.add(dim.getX(), 0, 0), min.add(dim.getX(), 0, dim.getZ()));
		t.drawTrace(v, min.add(dim.getX(), 0, dim.getZ()), min.add(0, 0, dim.getZ()));
		t.drawTrace(v, min.add(0, 0, dim.getZ()), min);
		t.drawTrace(v, max, max.sub(dim.getX(), 0, 0));
		t.drawTrace(v, max.sub(dim.getX(), 0, 0), max.sub(dim.getX(), 0, dim.getZ()));
		t.drawTrace(v, max.sub(dim.getX(), 0, dim.getZ()), max.sub(0, 0, dim.getZ()));
		t.drawTrace(v, max.sub(0, 0, dim.getZ()), max);
		t.drawTrace(v, min, min.add(0, dim.getY(), 0));
		t.drawTrace(v, min.add(dim.getX(), 0, 0), min.add(dim.getX(), dim.getY(), 0));
		t.drawTrace(v, min.add(dim.getX(), 0, dim.getZ()), min.add(dim));
		t.drawTrace(v, min.add(0, 0, dim.getZ()), min.add(0, dim.getY(), dim.getZ()));
	}
	public boolean isInside(Entity e) {
		if (!e.getLocation().getExtent().getUniqueId().equals(context)) return false;
		if (contains(e.getLocation().getPosition())) return true;
		Optional<AABB> aabb = e.getBoundingBox();
		if (!aabb.isPresent()) return false;
		return intersects(aabb.get());
	}
	/** return the extent this range was created for.
	 * From getWorld:  */
	public Optional<World> getExtent() {
		return Sponge.getServer().getWorld(context);
	}
	
	public static class Serializer implements TypeSerializer<Range> {

		@Override
		public Range deserialize(TypeToken<?> arg0, ConfigurationNode arg1) throws ObjectMappingException {
			Vector3i a = arg1.getNode("low").getValue(TypeToken.of(Vector3i.class));
			Vector3i b = arg1.getNode("high").getValue(TypeToken.of(Vector3i.class));
			Range ret = new Range(a,b, true);
			ret.context = UUID.fromString(arg1.getNode("context").getString());
			return ret;
		}

		@Override
		public void serialize(TypeToken<?> arg0, Range arg1, ConfigurationNode arg2) throws ObjectMappingException {
			arg2.getNode("low").setValue(TypeToken.of(Vector3i.class), arg1.getMin().toInt());
			arg2.getNode("high").setValue(TypeToken.of(Vector3i.class), arg1.getMax().toInt());
			arg2.getNode("context").setValue(arg1.context.toString());
		}
		
	}
}