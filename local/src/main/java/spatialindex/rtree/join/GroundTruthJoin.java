package spatialindex.rtree.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.geotools.data.FeatureReader;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import spatialindex.rtree.join.RTreeJoinQuery.JoinResultPair;
import utils.PropertiesReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public class GroundTruthJoin {
    public List<JoinResultPair> runJoin( ShapefileDataStore shpLayer1,  ShapefileDataStore shpLayer2) throws IllegalArgumentException, NoSuchElementException, IOException, ParseException {
        int numCacheGeometries = PropertiesReader.getInstance().getNumCacheGeometries();
        
        long time = System.currentTimeMillis();
        FeatureReader<SimpleFeatureType,SimpleFeature> readerLayer2 = shpLayer2.getFeatureReader();
        List<Feature> layer2Features = new ArrayList<Feature>();
        
        while(readerLayer2.hasNext()) {
            Feature feature = readerLayer2.next();
            layer2Features.add(feature);
        }
        
        FeatureReader<SimpleFeatureType,SimpleFeature> readerLayer1 = shpLayer1.getFeatureReader();
        List<JoinResultPair> result = new ArrayList<JoinResultPair>();
        int numSystemThreads = PropertiesReader.getInstance().getNumSystemThreads();
        ThreadPoolExecutor pool = new ThreadPoolExecutor(numSystemThreads, numSystemThreads * 2,
                10,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        
        while(readerLayer1.hasNext()) {
            Feature feature = readerLayer1.next();
            String layer1Id = feature.getIdentifier().getID().split("\\.")[1];
            pool.execute(new IntersectsThread(layer1Id, numCacheGeometries, layer2Features, result));
        }
        
        try {
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        
        System.out.println("Tempo: " +(System.currentTimeMillis() - time));
        return result;
    }
    
    private class IntersectsThread implements Runnable {
        String layer1Id;
        int numCacheGeometries;
        List<Feature> layer2Features;
        private List<JoinResultPair> result;
        
        public IntersectsThread(String layer1Id, int numCacheGeometries,
                List<Feature> layer2Features, List<JoinResultPair> result) {
            this.layer1Id = layer1Id;
            this.numCacheGeometries = numCacheGeometries;
            this.layer2Features = layer2Features;
            this.result = result;
        }

        @Override
        public void run() {
            try {
                List<Geometry> desmataPolygons = ProbabilisticGeometriesService.getCachedDesmataPolygons(layer1Id, numCacheGeometries);
                for(Feature featureLayer2: layer2Features) {
                    String layer2Id = featureLayer2.getIdentifier().getID().split("\\.")[1];
                    List<Geometry> vegetaPolygons = ProbabilisticGeometriesService.getCachedVegetaPolygons(layer2Id, numCacheGeometries);
                    int intersections = intersectsGeometries(desmataPolygons, vegetaPolygons);
                    if(intersections > 0) {
                        result.add(new JoinResultPair(layer1Id, layer2Id));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private int intersectsGeometries(List<Geometry> layer1Geometries, List<Geometry> layer2Geometries) {
            int intersections = 0;
            for(Geometry geom1: layer1Geometries) {
                for(Geometry geom2: layer2Geometries) {
                    if(geom1.intersects(geom2)) 
                        intersections++;
                }
            }
            
            return intersections;
        }
    }
}
