package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureReader;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public class CreateGeometryShapeFile {

    private static final double ERROR_METERS = JtsFactories.metersToDegrees(5000);
    
    private static String filePathDirLayer = "/home/savio/polygons/datasets";
    
    private static String filePathLayer1 = "/home/savio/Google Drive/Artigos/rtree/layers/desmatamento.shp";
    private static String filePathLayer2 = "/home/savio/Google Drive/Artigos/rtree/layers/vegetacao.shp";
    private static String filePathLayer3 = "/home/savio/Google Drive/Artigos/rtree/layers/queimada_pastagem.shp";
    
    private static String fileNameLayer1 = "desmata";
    private static String fileNameLayer2 = "vegeta";
    private static String fileNameLayer3 = "queimada";

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws IOException, ParseException {
        PropertiesReader.getInstance(args[0]);
        String distribution = PropertiesReader.getInstance().getDistribution().name();
        
        ShapefileDataStore shpLayer1 = new ShapefileDataStore(new File(filePathLayer1).toURL());
        ShapefileDataStore shpLayer2 = new ShapefileDataStore(new File(filePathLayer2).toURL());
        ShapefileDataStore shpLayer3 = new ShapefileDataStore(new File(filePathLayer3).toURL());
        
        
        writeGeometryShape(shpLayer1, filePathDirLayer +"/" +distribution +"/" +fileNameLayer1 +"_cache", filePathDirLayer +"/" +distribution +"/" +fileNameLayer1 +".shp");
        writeGeometryShape(shpLayer2, filePathDirLayer +"/" +distribution +"/" +fileNameLayer2 +"_cache", filePathDirLayer +"/" +distribution +"/" +fileNameLayer2 +".shp");
        writeGeometryShape(shpLayer3, filePathDirLayer +"/" +distribution +"/" +fileNameLayer3 +"_cache", filePathDirLayer +"/" +distribution +"/" +fileNameLayer3 +".shp");
        
    }
    
    private static Set<String> readCachedGeometryIds(String fileCacheDir) {
        Set<String> cachedIds = new HashSet<String>();
        
        File file = new File(fileCacheDir);
        for(String id: file.list())
            cachedIds.add(id);
        
        return cachedIds;
    }
    
    private static void verifyGeomError(Geometry geom, String fileCachePath) throws IOException, ParseException {
    	BufferedReader br = new BufferedReader(new FileReader(fileCachePath));
    	
    	String line = null;
    	while((line = br.readLine()) != null) {
    		Geometry wkt = JtsFactories.readWKT(line);
    		Coordinate[] geomCoordinates = geom.getCoordinates();
    		Coordinate[] wktCoordinates = wkt.getCoordinates();
    		
    		for(int i =0; i < geomCoordinates.length; i++) {
    			if (geomCoordinates[i].distance(wktCoordinates[i]) > ERROR_METERS) {
    				System.err.println("ERRO na geometria " +geom +"\t" +wkt +"\t" +geomCoordinates[i] +"\t" +wktCoordinates[i] +"\t" +ERROR_METERS +"\t" +fileCachePath);
    				System.exit(1);
    			}
    		}
    	}
    	
    	br.close();
    }
    
    public static void writeGeometryShape (ShapefileDataStore shpLayer, String fileCacheDir, String outputFilePathLayer ) throws IOException, ParseException {
        Set<String> cachedGeometryIds = readCachedGeometryIds(fileCacheDir);
        
        FeatureReader<SimpleFeatureType,SimpleFeature> readerLayer = shpLayer.getFeatureReader();
        List<Feature> features = new ArrayList<Feature>();
        SimpleFeatureType ft = null;
        
        while(readerLayer.hasNext()) {
            Feature feature = readerLayer.next();
            try {
                final String featureId = feature.getIdentifier().getID().split("\\.")[1];
                
                if (!cachedGeometryIds.contains(featureId))
                    continue;
                
                ft = (SimpleFeatureType) feature.getType();
//                Geometry geometry = getGeomOfFeature(feature, ft);
//                verifyGeomError(geometry, fileCacheDir +"/" +featureId);
                
                features.add(feature);
                
            } catch (Exception e) {
                System.err.println("Erro: " +feature.getIdentifier());
                e.printStackTrace();
                continue;
            }
        }
        
        writeShapeFile(outputFilePathLayer, ft, features);
    }
    
    private static void writeShapeFile(String outputFilePathLayer, SimpleFeatureType featureType, List<Feature> features) throws IOException {
        /*
         * Get an output file name and create the new shapefile
         */
        File newFile = new File(outputFilePathLayer);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore =  (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema(featureType);

        /*
         * You can comment out this line if you are using the createFeatureType method (at end of
         * class file) rather than DataUtilities.createType
         */
        newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
        
        /*
         * Write the features to the shapefile
         */
        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
        DefaultFeatureCollection collection = new DefaultFeatureCollection();
        for(Feature feature: features)
            collection.add((SimpleFeature) feature);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(collection);
                transaction.commit();

            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();

            } finally {
                transaction.close();
            }
        } else {
            System.out.println(typeName + " does not support read/write access");
            System.exit(1);
        }
    }
    
    public static Geometry getGeomOfFeature(Feature f,
            FeatureType featureType) throws ParseException {
        for (Property prop : f.getProperties())
            if (prop.getName().getURI().toLowerCase().intern()
                    .equals(featureType.getGeometryDescriptor().getName()
                            .toString())) {
                return (Geometry) prop.getValue();
            }

        return null;
    }
}
