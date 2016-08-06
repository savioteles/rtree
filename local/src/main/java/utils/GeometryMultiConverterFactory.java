package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

class GeometryMultiConverterFactory {

    interface IMultiConverter {

        Geometry createMultiGeomOfList(GeometryFactory gf, List<Geometry> list);

    }

    private static class MultiLineStringConverter implements IMultiConverter {

        @Override
        public Geometry createMultiGeomOfList(GeometryFactory gf,
                List<Geometry> list) {
            LineString[] listp = new LineString[list.size()];
            return gf.createMultiLineString(list.toArray(listp));
        }

    }

    private static class MultiPointConverter implements IMultiConverter {

        @Override
        public Geometry createMultiGeomOfList(GeometryFactory gf,
                List<Geometry> list) {
            Point[] listp = new Point[list.size()];
            return gf.createMultiPoint(list.toArray(listp));
        }

    }

    private static class MultiPolygonConverter implements IMultiConverter {

        @Override
        public Geometry createMultiGeomOfList(GeometryFactory gf,
                List<Geometry> list) {
            Polygon[] listp = new Polygon[list.size()];
            return gf.createMultiPolygon(list.toArray(listp));
        }

    }

    private static Map<Class<?>, IMultiConverter> mapMultiConverter;

    static {
        mapMultiConverter = new HashMap<Class<?>, IMultiConverter>();
        mapMultiConverter.put(LineString.class, new MultiLineStringConverter());
        mapMultiConverter.put(Polygon.class, new MultiPolygonConverter());
        mapMultiConverter.put(Point.class, new MultiPointConverter());
    }

    public static Geometry createMulti(GeometryFactory gf, Geometry... geoms) {
        if (geoms == null || geoms.length == 0)
            return null;

        if (geoms.length == 1 && geoms[0] instanceof GeometryCollection)
            return geoms[0];

        List<Geometry> list = new ArrayList<Geometry>();

        for (Geometry g : geoms)
            for (int i = 0; i < g.getNumGeometries(); i++)
                list.add(g.getGeometryN(i));

        return mapMultiConverter.get(list.get(0).getClass())
                .createMultiGeomOfList(gf, list);
    }
}
