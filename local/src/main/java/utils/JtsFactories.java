package utils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.TDistribution;
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
    
    private final static double originShift = 2 * Math.PI * 6378137 / 2.0;;

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
    
    public static boolean intersects(Envelope env1, Envelope env2, double meters, double gamma, double sd) {
    	Envelope envProb1 = changeEnvelopePointsProbabilistic(env1, meters, gamma, sd);
    	Envelope envProb2 = changeEnvelopePointsProbabilistic(env2, meters, gamma, sd);
    	return envProb1.intersects(envProb2);
    }
    
    private static Envelope changeEnvelopePointsProbabilistic(Envelope inputEnv, double meters, double gamma, double sd){
    	double d = shiftEnvelopeDistance(meters, gamma, sd);
    	double xMin = inputEnv.getMinX() - d;
    	double xMax = inputEnv.getMaxX() + d;
    	double yMin = inputEnv.getMinY() - d ;
    	double yMax = inputEnv.getMaxY() + d;
    	
    	return new Envelope(xMin, xMax, yMin, yMax);
    }
    
    private static double shiftEnvelopeDistance(double meters, double gamma, double sd) {
    	Distribution distributionType = PropertiesReader.getInstance().getDistribution();
        switch (distributionType) {
        case normal:
            return metersToDegrees(meters) + metersToDegrees(sd) * (Math.sqrt(1 / (1 - gamma)));
        case exponencial:
        	return metersToDegrees(meters) + metersToDegrees(meters) * (Math.sqrt(1 / (1 - gamma)));
        case chisquared:
            return metersToDegrees(meters) + metersToDegrees(sd) * (Math.sqrt(1 / (1 - gamma)));
        default:
            return 0;
        }
    }
    
    public static boolean intersects(Geometry geom1, Geometry geom2, double meters){
    	Geometry geom1Prob = changeGeometryPointsProbabilistic(geom1, meters);
    	Geometry geom2Prob = changeGeometryPointsProbabilistic(geom2, meters);
    	return geom1Prob.intersects(geom2Prob);
    }
    
    public static Geometry changeGeometryPointsProbabilistic(Geometry input, double meters) {
    	if(input instanceof Point)
    		return changeGeometryPointsProbabilistic((Point)input, meters);
    	if(input instanceof Polygon)
    		return changeGeometryPointsProbabilistic((Polygon)input, meters);
    	return changeGeometryPointsProbabilistic((MultiPolygon)input, meters);
    }
    
    private static Geometry changeGeometryPointsProbabilistic(Point input, double meters) {
    	Coordinate shiftPoint = shiftPoint(input.getCoordinate(), meters);
    	return gf.createPoint(shiftPoint);
    }
    
    private static Geometry changeGeometryPointsProbabilistic(MultiPolygon input, double meters) {
    	int numGeometries = input.getNumGeometries();
    	Polygon[] polygons = new Polygon[numGeometries];
    	
    	for(int i = 0; i < numGeometries; i++){
    		Polygon polygon = (Polygon) input.getGeometryN(i);
    		polygons[i] = (Polygon) changeGeometryPointsProbabilistic(polygon, meters);
    	}
    	
    	return gf.createMultiPolygon(polygons);
    }
    public static Geometry changeGeometryPointsProbabilistic(Polygon input, double meters) {
    	Coordinate[] coordinates = input.getCoordinates();
    	Coordinate[] shiftCoordinates = new Coordinate[coordinates.length];
    	
    	//first and last point must be same
    	shiftCoordinates[0] = shiftCoordinates[coordinates.length -1] = shiftPoint(coordinates[0], meters); 
    	for(int i = 1; i < coordinates.length - 1; i++){
    		Coordinate coordinate = coordinates[i];
    		shiftCoordinates[i] = shiftPoint(coordinate, meters);
    	}
    	
    	return gf.createPolygon(shiftCoordinates);
    }
    
    private static double getDistributionSample(double meters) {
        Distribution distributionType = PropertiesReader.getInstance().getDistribution();
        switch (distributionType) {
        case normal:
            distribution = new NormalDistribution(0, metersToDegrees(meters));
            return distribution.sample();
        case exponencial:
            distribution = new ExponentialDistribution(metersToDegrees(meters));
            return distribution.sample();
        case chisquared:
            distribution = new NormalDistribution(Math.sqrt(metersToDegrees(meters)), metersToDegrees(1));
            return Math.pow(distribution.sample(), 2);
        default:
            return 0;
        }
    }
    
    private static AbstractRealDistribution distribution;
    private static Coordinate shiftPoint(Coordinate input, double meters) {
    	double angle = ThreadLocalRandom.current().nextDouble(2 * Math.PI);
    	
    	double shift = getDistributionSample(meters);

    	double x_shift = shift * Math.cos(angle);
    	double y_shift = shift * Math.sin(angle);
    	
    	return new Coordinate(input.x + x_shift, input.y + y_shift);
    }
    
    public static double metersToDegrees(double meters) {

        return (meters / originShift) * 180.0;
    }
    
    public enum Distribution {
        normal, chisquared, exponencial
    }
    
    public static double calculateTC(int n, double gamma) {
        double quantileValue = (1 + gamma) / 2;
        TDistribution tDistribution = new TDistribution(n - 1);
        return tDistribution.inverseCumulativeProbability(quantileValue);
    }
}
