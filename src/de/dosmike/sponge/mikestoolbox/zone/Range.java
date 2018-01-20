package de.dosmike.sponge.mikestoolbox.zone;

import java.util.Optional;

import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.extent.Extent;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;

import de.dosmike.sponge.mikestoolbox.tracer.BoxTracer;

public class Range extends AABB {
	Extent context;
	
	/** by using the block position the high location will fall one block short. 
	 * A range with start position = end position would have a size of 0 if the high-block 
	 * is excluded. For this reason it will be included by default, making the size 
	 * 1x1x1 if a == b */
	public Range(Extent e, Vector3i a, Vector3i b) {
		super(a,b.add(1,1,1));
	}
	/** by using the block position the high location will fall one block short. 
	 * A range with start position = end position would have a size of 0 if the high-block 
	 * is excluded. For this reason it will be included by default, making the size 
	 * 1x1x1 if a == b */
	public Range(Extent e, Vector3i a, Vector3i b, boolean excludeHigh) {
		super(a,excludeHigh?b:b.add(1,1,1));
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
		if (!e.getLocation().getExtent().equals(context)) return false;
		if (contains(e.getLocation().getPosition())) return true;
		Optional<AABB> aabb = e.getBoundingBox();
		if (!aabb.isPresent()) return false;
		return intersects(aabb.get());
	}
	/** return the extent this range was created for */
	public Extent getExtent() {
		return context;
	}
}