package utils;

import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

class GeometryRemoveDuplicateConsecutePointsFactory {

    private static final class GeometryCollectionRemoveDuplicate implements IMultiConverter {

        @Override
        public Geometry removeDuplicateOfGeometry(GeometryFactory gf,
                Geometry geom) {
            Geometry[] array = new Geometry[geom.getNumGeometries()];
            for (int i = 0; i < array.length; i++)
                array[i] = removeDuplicate(gf, geom.getGeometryN(i));

            return GeometryMultiConverterFactory.createMulti(gf, array);
        }

    }

    interface IMultiConverter {

        Geometry removeDuplicateOfGeometry(GeometryFactory gf, Geometry geom);

    }

    private static final class LineStringRemoveDuplicate implements IMultiConverter {

        @Override
        public Geometry removeDuplicateOfGeometry(GeometryFactory gf,
                Geometry geom) {
            LineString l = (LineString) geom;

            Coordinate[] coordinates = removeRepeatedPoints(gf,
                    l.getCoordinates());

            return gf.createLineString(coordinates);
        }

    }

    // Esta operação não é necessária para pontos uma vez que não existe
    // duplicação em um único ponto
    private static final class PointRemoveDuplicate implements IMultiConverter {

        @Override
        public Geometry removeDuplicateOfGeometry(GeometryFactory gf,
                Geometry geom) {
            return geom;
        }

    }

    private static final class PolygonRemoveDuplicate implements IMultiConverter {

        @Override
        public Geometry removeDuplicateOfGeometry(GeometryFactory gf,
                Geometry geom) {
            Polygon p = (Polygon) geom;

            Coordinate[] coordinates = CoordinateArrays
                    .removeRepeatedPoints(p.getExteriorRing().getCoordinates());
            LinearRing exteriorRing = gf.createLinearRing(coordinates);

            LinearRing[] holes = new LinearRing[p.getNumInteriorRing()];
            for (int i = 0; i < holes.length; i++) {
                coordinates = CoordinateArrays.removeRepeatedPoints(
                        p.getInteriorRingN(i).getCoordinates());
                holes[i] = gf.createLinearRing(coordinates);
            }

            return gf.createPolygon(exteriorRing, holes);
        }

    }

    private static Map<Class<?>, IMultiConverter> mapRemoveDuplicate;

    static {
        mapRemoveDuplicate = new HashMap<Class<?>, IMultiConverter>();
        mapRemoveDuplicate.put(LineString.class,
                new LineStringRemoveDuplicate());
        mapRemoveDuplicate.put(Polygon.class, new PolygonRemoveDuplicate());
        mapRemoveDuplicate.put(Point.class, new PointRemoveDuplicate());
        mapRemoveDuplicate.put(MultiLineString.class,
                new GeometryCollectionRemoveDuplicate());
        mapRemoveDuplicate.put(MultiPolygon.class,
                new GeometryCollectionRemoveDuplicate());
        mapRemoveDuplicate.put(MultiPoint.class,
                new GeometryCollectionRemoveDuplicate());
    }

    public static Geometry removeDuplicate(GeometryFactory gf, Geometry geom) {
        if (geom == null)
            return null;

        return mapRemoveDuplicate.get(geom.getClass())
                .removeDuplicateOfGeometry(gf, geom);
    }

    private static final Coordinate[] removeRepeatedPoints(GeometryFactory gf,
            Coordinate[] coordinates) {
        return CoordinateArrays.removeRepeatedPoints(coordinates);
    }
}
