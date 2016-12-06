package spatialindex.rtree.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.geotools.data.FeatureReader;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

import spatialindex.rtree.join.RTreeJoinQuery.JoinResultPair;
import utils.JtsFactories;
import utils.PropertiesReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public class GroundTruthJoin {
    public Queue<JoinResultPair> runJoin( ShapefileDataStore shpLayer1,  ShapefileDataStore shpLayer2, int numCacheGeometries) throws IllegalArgumentException, NoSuchElementException, IOException, ParseException {
        long time = System.currentTimeMillis();
        FeatureReader<SimpleFeatureType,SimpleFeature> readerLayer2 = shpLayer2.getFeatureReader();
        List<Feature> layer2Features = new ArrayList<Feature>();
        
        while(readerLayer2.hasNext()) {
            Feature feature = readerLayer2.next();
            layer2Features.add(feature);
        }
        
        FeatureReader<SimpleFeatureType,SimpleFeature> readerLayer1 = shpLayer1.getFeatureReader();
        Queue<JoinResultPair> result = new LinkedBlockingQueue<JoinResultPair>();
        int numSystemThreads = PropertiesReader.getInstance().getNumSystemThreads();
        ThreadPoolExecutor pool = new ThreadPoolExecutor(numSystemThreads, numSystemThreads * 2,
                10,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        
        while(readerLayer1.hasNext()) {
            Feature feature = readerLayer1.next();
            String layer1Id = feature.getIdentifier().getID().split("\\.")[1];
            Geometry geometry = getGeomOfFeature(feature, feature.getType(), false);
            runJoin(layer1Id, numCacheGeometries, geometry, layer2Features, result);
//            pool.execute(new IntersectsThread(layer1Id, geometry, numCacheGeometries, layer2Features, result));
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
        Geometry layer1Geom;
        List<Feature> layer2Features;
        private Queue<JoinResultPair> result;
        
        public IntersectsThread(String layer1Id, Geometry layer1Geom, int numCacheGeometries,
                List<Feature> layer2Features, Queue<JoinResultPair> result) {
        	this.layer1Geom = layer1Geom;
            this.layer1Id = layer1Id;
            this.numCacheGeometries = numCacheGeometries;
            this.layer2Features = layer2Features;
            this.result = result;
        }

        @Override
        public void run() {
            try {
                runJoin(layer1Id, numCacheGeometries, layer1Geom, layer2Features, result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        
    }
    
    public static void runJoin(String layer1Id, int numCacheGeometries, Geometry layer1Geom, List<Feature> layer2Features, Queue<JoinResultPair> result) throws IOException, ParseException {
//    	List<Geometry> desmataPolygons = ProbabilisticGeometriesService.getDesmataPolygons(layer1Id, layer1Geom, numCacheGeometries);
//        for(Feature featureLayer2: layer2Features) {
//            String layer2Id = featureLayer2.getIdentifier().getID().split("\\.")[1];
//            Geometry layer2Geom = getGeomOfFeature(featureLayer2, featureLayer2.getType(), false);
//            List<Geometry> vegetaPolygons = ProbabilisticGeometriesService.getCachedVegetaPolygons(layer2Id, layer2Geom, numCacheGeometries);
//            int total = desmataPolygons.size();
//            int intersections = intersectsGeometries(desmataPolygons, vegetaPolygons);
//            if(intersections > 0) {
//                result.add(new JoinResultPair(layer1Id, layer2Id, total - intersections, intersections));
//            }
//        }
    	for(Feature featureLayer2: layer2Features) {
    		String layer2Id = featureLayer2.getIdentifier().getID().split("\\.")[1];
          Geometry layer2Geom = getGeomOfFeature(featureLayer2, featureLayer2.getType(), false);
          if(layer1Geom.intersects(layer2Geom)) {
        	  if(layer1Id.equals("1007"))
        		  System.out.println(layer1Geom  +";desmata\n" +layer2Geom +";vegeta\n");
            result.add(new JoinResultPair(layer1Id, layer2Id, 20, 20));
        }
    	}
    }
    
    private static int intersectsGeometries(List<Geometry> layer1Geometries, List<Geometry> layer2Geometries) {
        int intersections = 0;
        for(int i = 0; i < layer1Geometries.size(); i++) {
            Geometry geom1 = layer1Geometries.get(i);
            Geometry geom2 = layer2Geometries.get(i);
            if(geom1.intersects(geom2)) 
                intersections++;
        }
        
        return intersections;
    }
    
    public static Geometry getGeomOfFeature(Feature f,
            FeatureType featureType, boolean shiftPoint) {
		int errorInMeters = PropertiesReader.getInstance().getErrorInMeters();
        for (Property prop : f.getProperties())
            if (prop.getName().getURI().toLowerCase().intern()
                    .equals(featureType.getGeometryDescriptor().getName()
                            .toString())) {
                Geometry geometry = (Geometry) prop.getValue();
                if(shiftPoint) {
                    Geometry probabilisticGeom = JtsFactories.changeGeometryPointsProbabilistic(geometry, errorInMeters);
                    if (!probabilisticGeom.isValid()) {
                        probabilisticGeom = geometry.convexHull();
                        probabilisticGeom = JtsFactories.changeGeometryPointsProbabilistic(
                                probabilisticGeom, errorInMeters);
                    }
                    return probabilisticGeom;
                }
                return geometry;
            }

        return null;
    }
}
