package utils;

import java.io.File;
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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.io.ParseException;

public class CreateGeometryShapeFile {

    private static String filePathLayer1 = "/home/savio/Google Drive/Artigos/rtree/layers/desmatamento.shp";
    private static String outputFilePathLayer1 = "/home/savio/Google Drive/Artigos/rtree/layers/desmatamento_final.shp";
    
    private static String filePathLayer2 = "/home/savio/Google Drive/Artigos/rtree/layers/vegetacao.shp";
    private static String outputFilePathLayer2 = "/home/savio/Google Drive/Artigos/rtree/layers/vegetacao_final.shp";
    
    private static String filePathLayer3 = "/home/savio/Google Drive/Artigos/rtree/layers/queimada_pastagem.shp";
    private static String outputilePathLayer3 = "/home/savio/Google Drive/Artigos/rtree/layers/queimada_pastagem_final.shp";
    
    private static String fileCachePathLayer1 = "/home/savio/polygons/desmata_cache";
    private static String fileCachePathLayer2 = "/home/savio/polygons/vegeta_cache";
    private static String fileCachePathLayer3 = "/home/savio/polygons/queimada_cache";

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws IOException, ParseException {
        ShapefileDataStore shpLayer1 = new ShapefileDataStore(new File(filePathLayer1).toURL());
        ShapefileDataStore shpLayer2 = new ShapefileDataStore(new File(filePathLayer2).toURL());
        ShapefileDataStore shpLayer3 = new ShapefileDataStore(new File(filePathLayer3).toURL());
        
        writeGeometryShape(shpLayer1, fileCachePathLayer1, outputFilePathLayer1);
        writeGeometryShape(shpLayer2, fileCachePathLayer2, outputFilePathLayer2);
        writeGeometryShape(shpLayer3, fileCachePathLayer3, outputilePathLayer3);
        
    }
    
    private static Set<String> readCachedGeometryIds(String fileCacheDir) {
        Set<String> cachedIds = new HashSet<String>();
        
        File file = new File(fileCacheDir);
        for(String id: file.list())
            cachedIds.add(id);
        
        return cachedIds;
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
                
                features.add(feature);
                ft = (SimpleFeatureType) feature.getType();
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
}
