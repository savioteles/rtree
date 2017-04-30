package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.FeatureReader;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public class GenerateCachedPolygons {

    private static final int TRIES = 20;
    private static final int CACHED_GEOMS = 1000;
    private static String filePathLayer1 = "/home/savio/Welder/rtree/layers/desmatamento.shp";
    private static String filePathLayer2 = "/home/savio/Welder/rtree/layers/vegetacao.shp";
    private static String filePathLayer3 = "/home/savio/Welder/rtree/layers/queimada_pastagem.shp";
    
    private static String fileCachePathLayer = "/home/savio/polygons/cache";
    
    private static String fileCacheNameLayer1 = "desmata_cache";
    private static String fileCacheNameLayer2 = "vegeta_cache";
    private static String fileCacheNameLayer3 = "queimada_cache";

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws IOException, ParseException {
    	PropertiesReader.getInstance(args[0]);
    	String name = PropertiesReader.getInstance().getDistribution().name();
    	File cacheDir = new File(fileCachePathLayer +"/" +name);
    	
    	if(!cacheDir.exists())
    	    cacheDir.mkdirs();
    	
        ShapefileDataStore shpLayer1 = new ShapefileDataStore(new File(filePathLayer1).toURL());
        ShapefileDataStore shpLayer2 = new ShapefileDataStore(new File(filePathLayer2).toURL());
        ShapefileDataStore shpLayer3 = new ShapefileDataStore(new File(filePathLayer3).toURL());
        
        writeGeometry(shpLayer1, cacheDir.getAbsolutePath() +"/" +fileCacheNameLayer1);
        writeGeometry(shpLayer2, cacheDir.getAbsolutePath() +"/" +fileCacheNameLayer2);
        writeGeometry(shpLayer3, cacheDir.getAbsolutePath() +"/" +fileCacheNameLayer3);
        
    }
    
    public static void writeGeometry (ShapefileDataStore shpLayer, String fileCacheDir ) throws IOException, ParseException {
        if(!new File(fileCacheDir).exists())
            new File(fileCacheDir).mkdirs();
        FeatureReader<SimpleFeatureType,SimpleFeature> readerLayer2 = shpLayer.getFeatureReader();
        
        while(readerLayer2.hasNext()) {
            Feature feature = readerLayer2.next();
            try {
                List<Geometry> probabilisticGeometries = getProbabilisticGeometries(feature, feature.getType());
                
                String fileName = fileCacheDir +"/" +feature.getIdentifier().getID().split("\\.")[1];
                BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
                for (Geometry geom: probabilisticGeometries)
                    bw.write(geom.toText() +"\n");
                bw.close();
            } catch (Exception e) {
                System.err.println("Erro: " +feature.getIdentifier());
                e.printStackTrace();
                continue;
            }
        }
    }
    
    public static List<Geometry> getProbabilisticGeometries(Feature f, FeatureType featureType) throws Exception {
        try {
            List<Geometry> geometries = new ArrayList<Geometry>();
            for(int i = 0; i < CACHED_GEOMS; i++) {
                Geometry geometry = getGeomOfFeature(f, featureType, true);
                geometries.add(geometry);
            }
            return geometries;
        } catch (Exception e) {
            throw e;
        }
    }
    
    public static Geometry getGeomOfFeature(Feature f,
            FeatureType featureType, boolean shiftPoint) throws ParseException {
        int errorInMeters = PropertiesReader.getInstance().getErrorInMeters();
        for (Property prop : f.getProperties())
            if (prop.getName().getURI().toLowerCase().intern()
                    .equals(featureType.getGeometryDescriptor().getName()
                            .toString())) {
                Geometry geometry = (Geometry) prop.getValue();
                if(shiftPoint) {
                    Geometry probabilisticGeom = JtsFactories.changeGeometryPointsProbabilistic(geometry, errorInMeters);
                    int tries = 0;
                    while (!probabilisticGeom.isValid()) {
                        if (++tries >= TRIES)
                            throw new ParseException("The probabilistic geometry " +f.getIdentifier().getID() +" is not valid.");
                        probabilisticGeom = JtsFactories.changeGeometryPointsProbabilistic(geometry, errorInMeters);
                        
                    }
                    return probabilisticGeom;
                }
                return geometry;
            }

        return null;
    }
}
