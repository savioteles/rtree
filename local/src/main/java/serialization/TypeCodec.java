package serialization;

import java.util.ArrayList;
import java.util.HashMap;

import javax.lang.model.type.NullType;

import utils.DataLoaderUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class TypeCodec {

    private static HashMap<Integer, Class<?>> types;

    static {
        loadTypes();
    }

    public static Class<?> decodeClass(int value) {
        Class<?> clazz = types.get(value);

        if (clazz == null) {
            loadTypes();
            clazz = types.get(value);
        }

        if (clazz == null)
            throw new RuntimeException(
                    "Error on static configuration: class not found on map types. Include type on map types.");

        return clazz;
    }

    /**
     * 
     * @param c
     * @return
     */
    public static int encodeClass(Class<?> clazz) {
        return clazz.getName().hashCode();
    }

    private static void loadTypes() {
        types = new HashMap<Integer, Class<?>>();

        try {
            Class<?>[] serializableClasses = DataLoaderUtils
                    .getClassesOnPackage("spatialindex",
                            AbstractSerializeObject.class);

            for (Class<?> clazz : serializableClasses)
                types.put(encodeClass(clazz), clazz);

            Class<?>[] adicionalClasses = new Class[] {
                    Integer.class,
                    Long.class,
                    Short.class,
                    Double.class,
                    Float.class,
                    Boolean.class,
                    Character.class,
                    Byte.class,
                    NullType.class,
                    String.class,

                    int[].class,
                    short[].class,
                    long[].class,
                    float[].class,
                    double[].class,
                    boolean[].class,
                    char[].class,
                    byte[].class,

                    ArrayList.class,
                    HashMap.class,
                    Envelope.class,
                    Geometry.class,
                    Point.class,
                    LineString.class,
                    Polygon.class,
                    MultiPoint.class,
                    MultiLineString.class,
                    MultiPolygon.class };

            for (Class<?> clazz : adicionalClasses)
                types.put(encodeClass(clazz), clazz);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
