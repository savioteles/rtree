package utils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.type.FeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.operation.valid.IsValidOp;
import com.vividsolutions.jts.operation.valid.TopologyValidationError;

public class JtsFactories {

    public static int WGS84_srid = 4326;
    public static int SphericalMercator_srid = 3857;

    private static ThreadLocal<WKBReader> wkbReader;
    private static ThreadLocal<WKBWriter> wkbWriter;

    private static PrecisionModel PRECISION_MODEL = new PrecisionModel(
            PrecisionModel.FLOATING);

    static {
        // WKBReader nao é thread-safe
        wkbReader = new ThreadLocal<WKBReader>() {
            @Override
            protected WKBReader initialValue() {
                return new WKBReader(gf);
            };
        };

        // WKBWriter nao é thread-safe
        wkbWriter = new ThreadLocal<WKBWriter>() {
            @Override
            protected WKBWriter initialValue() {
                return new WKBWriter();
            };
        };
    }

    private static final byte[] EMPTYGEOM = new byte[] { 0 };

    /**
     * Fábrica para construção de Geometrias
     */
    private static GeometryFactory gf = new GeometryFactory(PRECISION_MODEL,
            WGS84_srid);

    public static Envelope convertProjection(int projectionSource,
            int projectionDestine,
            Envelope envelope)
                    throws FactoryException, MismatchedDimensionException,
                    TransformException {
        Geometry convertProjection = convertProjection(projectionSource,
                projectionDestine, JtsFactories.toGeometry(envelope));
        return convertProjection.getEnvelopeInternal();
    }

    public static Geometry convertProjection(int projectionSource,
            int projectionDestine,
            Geometry geom)
                    throws FactoryException, MismatchedDimensionException,
                    TransformException {
        if (projectionDestine == projectionSource)
            return geom;

        CoordinateReferenceSystem sourceCRS = JtsFactories
                .sridToCRS(projectionSource, true);
        CoordinateReferenceSystem targetCRS = JtsFactories
                .sridToCRS(projectionDestine, true);

        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
        Geometry newGeo = JTS.transform(geom, transform);

        return newGeo;
    }

    public static Envelope convertProjection(String wktSource,
            int projectionDestine,
            Envelope envelope)
                    throws FactoryException, MismatchedDimensionException,
                    TransformException {
        Geometry convertProjection = convertProjection(wktSource,
                projectionDestine, JtsFactories.toGeometry(envelope));
        return convertProjection.getEnvelopeInternal();
    }

    public static Geometry convertProjection(String wktSource,
            int projectionDestine,
            Geometry geom)
                    throws FactoryException, MismatchedDimensionException,
                    TransformException {
        CoordinateReferenceSystem sourceCRS = wktToSrid(wktSource);
        CoordinateReferenceSystem targetCRS = JtsFactories
                .sridToCRS(projectionDestine, true);

        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS,
                true);
        Geometry newGeo = JTS.transform(geom, transform);

        return newGeo;
    }

    public static Geometry createMulti(Geometry... geoms) {
        return GeometryMultiConverterFactory.createMulti(gf, geoms);
    }

    public static Point createPoint(Coordinate coordinate) {
        return gf.createPoint(coordinate);
    }

    public static Point createPoint(double latitude, double longitude) {
        return createPoint(new Coordinate(latitude, longitude));
    }

    public static int CRSToSrid(CoordinateReferenceSystem crs) {
        return Integer
                .parseInt(crs.getIdentifiers().iterator().next().getCode());
    }

    public static Geometry geomBuffer(Geometry geom, double buffer) {

        if (geom.isRectangle()) {
            Envelope env = new Envelope(geom.getEnvelopeInternal());
            env.expandBy(buffer);
            return JtsFactories.toGeometry(env);
        } else
            return geom.buffer(buffer);
    }

    public static Geometry geometryCorrection(Geometry geom)
            throws Exception {
        geom = removeTopologyErrors(geom);
        geom = removeDuplicateConsecutivePoints(geom);
        return geom.norm();
    }

    public static Envelope getEnvelope(String text) {
        text = text.substring(4, text.length() - 1);
        StringTokenizer points = new StringTokenizer(text, ",");
        StringTokenizer x = new StringTokenizer(points.nextToken(), ":");
        StringTokenizer y = new StringTokenizer(points.nextToken(), ":");
        double y1 = Double.parseDouble(y.nextToken());
        double y2 = Double.parseDouble(y.nextToken());
        double x1 = Double.parseDouble(x.nextToken());
        double x2 = Double.parseDouble(x.nextToken());
        return new Envelope(x1, x2, y1, y2);
    }

    public static String getGeomKey(byte[] wkb) throws ParseException {
        return getGeomKey(unserializeGeometry(wkb));
    }

    public static String getGeomKey(Geometry geom) {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        sha256.update(serialize(geom.norm()));
        return ConvertUtils.string2Hexa(sha256.digest());
    }

    public static boolean isPoint(FeatureType ft) {
        return ft.getGeometryDescriptor().getType().toString().toLowerCase()
                .contains("point");
    }

    public static Geometry readJson(String json) throws IOException {
        GeometryJSON gjson = new GeometryJSON(
                PRECISION_MODEL.getMaximumSignificantDigits());
        Reader reader = new StringReader(json);
        return gjson.read(reader);
    }

    public static Geometry readWKT(String text) throws ParseException {
        return new WKTReader(gf).read(text);
    }

    public static Geometry removeDuplicateConsecutivePoints(Geometry geom) {
        return GeometryRemoveDuplicateConsecutePointsFactory.removeDuplicate(gf,
                geom);
    }

    public static Geometry removeTopologyErrors(Geometry geom)
            throws Exception {
        if (geom.isValid())
            return geom;

        if (geom instanceof MultiPolygon || geom instanceof Polygon) {
            Geometry fixedGeom = geom.buffer(0);
            if (fixedGeom.isEmpty())
                fixedGeom = geom.buffer(1 / (double) PRECISION_MODEL
                        .getMaximumSignificantDigits());

            if (geom instanceof MultiPolygon)
                fixedGeom = createMulti(fixedGeom);

            geom = fixedGeom;
        }

        IsValidOp validOp = new IsValidOp(geom);
        TopologyValidationError err = validOp.getValidationError();
        if (err != null)
            throw new IOException(err.toString());

        return geom;
    }

    public static byte[] serialize(Envelope envelope) {
        Geometry g = gf.toGeometry(envelope);

        if (g.isEmpty())
            return EMPTYGEOM;

        return wkbWriter.get().write(g);
    }

    public static byte[] serialize(Geometry geom) {
        WKBWriter wkbw = new WKBWriter();

        if (geom.isEmpty())
            return EMPTYGEOM;

        return wkbw.write(geom);
    }

    public static CoordinateReferenceSystem sridToCRS(int srid, boolean force)
            throws NoSuchAuthorityCodeException, FactoryException {
        return CRS.decode("EPSG:" + srid, force);
    }

    public static Geometry toGeometry(Envelope env) {
        return gf.toGeometry(env);
    }

    public static String toJson(Geometry geom) throws IOException {
        GeometryJSON gjson = new GeometryJSON(
                PRECISION_MODEL.getMaximumSignificantDigits());
        StringWriter writer = new StringWriter();
        gjson.write(geom, writer);
        return writer.toString();
    }

    public static Envelope unserializeEnvelope(byte[] rawdata)
            throws ParseException {
        if (Arrays.equals(EMPTYGEOM, rawdata))
            return new Envelope();

        Geometry g = wkbReader.get().read(rawdata);
        return g.getEnvelopeInternal();
    }

    public static Geometry unserializeGeometry(byte[] rawdata)
            throws ParseException {
        if (Arrays.equals(EMPTYGEOM, rawdata))
            return gf.toGeometry(new Envelope());

        return wkbReader.get().read(rawdata);
    }

    public static CoordinateReferenceSystem wktToSrid(String wkt)
            throws FactoryException {
        return CRS.parseWKT(wkt);
    }

    private JtsFactories() {
    }
}
