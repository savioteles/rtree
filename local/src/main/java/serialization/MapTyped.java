package serialization;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MapTyped extends AbstractSerializeObject {

    /**
     * 
     */
    private static final long serialVersionUID = -4618239572983479100L;
    private static final int MAPOFTYPES = 0;
    private transient Map<String, Class<?>> mapOfClass = null;

    public MapTyped() {
    }

    public MapTyped(Map<String, Class<?>> objMap) {

        Map<String, Integer> map = new LinkedHashMap<String, Integer>();

        for (Entry<String, Class<?>> entry : objMap.entrySet()) {
            int value = TypeCodec.encodeClass(entry.getValue());
            try {
                TypeCodec.decodeClass(value);
            } catch (RuntimeException e) {
                throw new RuntimeException("Class " + entry.getValue().getName()
                        + " was not maped.", e);
            }
            map.put(entry.getKey(), value);
        }

        setParams(map);
    }

    public Map<String, Class<?>> getTypes() {
        if (mapOfClass == null) {
            Map<String, Integer> param = getMapParam(MAPOFTYPES, String.class,
                    Integer.class);
            mapOfClass = new LinkedHashMap<String, Class<?>>();

            for (Entry<String, Integer> entry : param.entrySet())
                mapOfClass.put(entry.getKey(),
                        TypeCodec.decodeClass(entry.getValue()));
        }

        return mapOfClass;
    }
}
