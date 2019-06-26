package de.dosmike.sponge.mikestoolbox.database;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Requires exactly 60 bytes for column size */
public class H2LocationSerializer implements H2Serializer<Location<World>> {
    @Override
    public void serialize(Location<World> a, OutputStream os) {
        try {
            ByteBuffer bb = ByteBuffer.allocate(60);
            bb.put(a.getExtent().getUniqueId().toString().getBytes(StandardCharsets.US_ASCII));
            bb.putDouble(a.getX());
            bb.putDouble(a.getY());
            bb.putDouble(a.getZ());
            os.write(bb.array());
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                os.close();
            } catch (Exception ignore) {/**/}
        }
    }

    @Override
    public Location<World> deserialize(InputStream is) {
        try {
            byte[] raw = new byte[60];
            is.read(raw);
            ByteBuffer bb = ByteBuffer.wrap(raw);
            bb.rewind();
            byte[] w = new byte[36];
            bb.get(w);
            UUID world = UUID.fromString(new String(w, StandardCharsets.US_ASCII));
            Location<World> built =
                    Sponge.getServer().loadWorld(
                            world
                    ).get()
                            .getLocation(new Vector3d(bb.getDouble(), bb.getDouble(), bb.getDouble()));

            return built;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                is.close();
            } catch (Exception ignore) {/**/}
        }

    }
}
