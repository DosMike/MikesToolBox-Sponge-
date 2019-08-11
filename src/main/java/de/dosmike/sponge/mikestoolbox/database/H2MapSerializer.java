package de.dosmike.sponge.mikestoolbox.database;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class H2MapSerializer<K,V> implements H2Serializer<Map<K,V>> {
    @Override
    public void serialize(Map<K, V> a, OutputStream os) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeInt(a.size());
            for (Map.Entry<K, V> e : a.entrySet()) {
                oos.writeObject(e.getKey());
                oos.writeObject(e.getValue());
            }
            oos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                os.flush();
                os.close();
            } catch (Exception ignore) {/**/}
        }
    }

    @Override
    public Map<K, V> deserialize(InputStream is) {
        try {
            ObjectInputStream ois = new ObjectInputStream(is);
            Map<K,V> result = new HashMap<>();
            int s = ois.readInt();
            while (result.size() < s && ois.available() > 0) {
                K key = (K) ois.readObject();
                V value = (V) ois.readObject();
                result.put(key,value);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                is.close();
            } catch (Exception ignore) {/**/}
        }

    }
}
