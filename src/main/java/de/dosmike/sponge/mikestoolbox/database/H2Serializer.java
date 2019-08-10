package de.dosmike.sponge.mikestoolbox.database;

import java.io.InputStream;
import java.io.OutputStream;

public interface H2Serializer<T> {
    /** turn an object into a blob store-able byte array */
    void serialize(T a, OutputStream os);
    /** turn an blob byte array back into an actual object */
    T deserialize(InputStream is);
}
