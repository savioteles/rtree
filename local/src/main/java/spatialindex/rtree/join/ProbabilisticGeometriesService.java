package spatialindex.rtree.join;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.geotools.util.LRULinkedHashMap;

import utils.JtsFactories;
import utils.PropertiesReader;

import com.google.common.base.Strings;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public class ProbabilisticGeometriesService {
    private static final String layer1CacheFolder = PropertiesReader.getInstance().getCacheGeometrieslayer1Path();
    private static final String layer2CacheFolder = PropertiesReader.getInstance().getCacheGeometrieslayer2Path();
    
    private static final String layer1Name = PropertiesReader.getInstance().getLayer1Name();
    private static final String layer2Name = PropertiesReader.getInstance().getLayer2Name();
    
    private static final int MAX_CACHE_SIZE = PropertiesReader.getInstance().getMaxCacheEntries();
    private static final int ERROR_METERS = PropertiesReader.getInstance().getErrorInMeters();
    
    private static LRULinkedHashMap<String, List<Geometry>> cache = new LRULinkedHashMap<String, List<Geometry>>(MAX_CACHE_SIZE, 0.75f, false, MAX_CACHE_SIZE);
    
    private static final int NUM_CACHE_GEOMETRIES = PropertiesReader.getInstance().getNumCacheGeometries();
    private static LRULinkedHashMap<String, Map<Integer, Geometry>> randomCache = new LRULinkedHashMap<String, Map<Integer, Geometry>>(MAX_CACHE_SIZE, 0.75f, false, MAX_CACHE_SIZE);
    
    public static Geometry getLayer1ProbabilisticGeometry(Geometry originalGeom, String id, int index) throws IOException, ParseException {
        return getProbabilisticGeometry(originalGeom, layer1Name, id, index);
    }
    
    public static Geometry getLayer2ProbabilisticGeometry(Geometry originalGeom, String id, int index) throws IOException, ParseException {
        return getProbabilisticGeometry(originalGeom, layer2Name, id, index);
    }
    
    public static Geometry getCachedLayer1ProbabilisticGeometry(Geometry originalGeom, String id, int index, int maxSize) throws IOException, ParseException {
    	List<Geometry> layer1CachedPolygons = getLayer1CachedPolygons(id, originalGeom, maxSize);
    	return layer1CachedPolygons.get(index);
    }
    
    public static Geometry getCachedLayer2ProbabilisticGeometry(Geometry originalGeom, String id, int index, int maxSize) throws IOException, ParseException {
    	List<Geometry> layer2CachedPolygons = getLayer2CachedPolygons(id, originalGeom, maxSize);
    	return layer2CachedPolygons.get(index);
    }
    
    private static Geometry getProbabilisticGeometry(Geometry originalGeom, String layer, String id, int index) throws IOException, ParseException {
        
        String cacheId = layer +id;
        List<Geometry> geometries = cache.get(cacheId);
        if(geometries != null && geometries.size() > index)
            return geometries.get(index);
        
        synchronized (cacheId.intern()) {   
            geometries = cache.get(cacheId);
            if(geometries != null && geometries.size() > index)
                return geometries.get(index);
            if(geometries == null) {
                geometries = new ArrayList<Geometry>();
                cache.put(cacheId, geometries);
            }
            Geometry probabilisticGeom = JtsFactories.changeGeometryPointsProbabilistic(originalGeom, ERROR_METERS);
            if (!probabilisticGeom.isValid()) {
                throw new ParseException("The probabilistic geometry " +cacheId +" is not valid.");
            }
            geometries.add(probabilisticGeom);
            
            return probabilisticGeom;
        }
    }
    
    public static List<Geometry> getLayer1CachedPolygons(String id, Geometry geom, int size) throws IOException, ParseException {
        return getPolygons(layer1CacheFolder, layer1Name, geom, id, size, true);
    }
    
    public static List<Geometry> getLayer1Polygons(String id, Geometry geom, int size) throws IOException, ParseException {
        return getPolygons(layer1CacheFolder, layer1Name, geom, id, size, false);
    }
    
    public static List<Geometry> getLayer2CachedPolygons(String id, Geometry geom, int size) throws IOException, ParseException {
        return getPolygons(layer2CacheFolder, layer2Name, geom, id, size, true);
    }
    
    public static List<Geometry> getLayer2Polygons(String id, Geometry geom, int size) throws IOException, ParseException {
        return getPolygons(layer2CacheFolder, layer2Name, geom, id, size, false);
    }
    
    private static List<Geometry> getPolygons(String folderPath, String layer, Geometry originalGeom, String id, int size, boolean cached) throws IOException, ParseException {
        String cacheId = layer +id;
        List<Geometry> geometries = null;
        
        if(cached) {
            geometries = cache.get(cacheId);
            if(geometries != null && geometries.size() >= size)
                return geometries;
        }
        
        if(Strings.isNullOrEmpty(folderPath)) {
        	if(geometries == null)
        		geometries = new ArrayList<Geometry>();
        	
        	for(int i = 0; i < size; i++) {
        		Geometry probabilisticGeometry = getProbabilisticGeometry(originalGeom, layer, cacheId, i);
        		geometries.add(probabilisticGeometry);
        		
        		if(geometries.size() >= size)
        			break;
        	}
        	
        	if(cached)
        	    cache.put(cacheId, geometries);
        	return geometries;
        }
        
        if(geometries == null) {
            geometries = new ArrayList<Geometry>();
        }
        
        BufferedReader reader = new BufferedReader(new FileReader(folderPath +"/"+id));
        
        for(int i = geometries.size(); i < size; i++) {
            String line = reader.readLine();
            Geometry geom = JtsFactories.readWKT(line);
            geometries.add(geom);
        }
        
        if(cached)
            cache.put(cacheId, geometries);
        reader.close();
        
        return geometries;
    } 
    
    public static Collection<Geometry> getRandomCachedPolygonsLayer1(String id, int size) throws IOException, ParseException {
        return getRandomPolygons(layer1CacheFolder, layer1Name, id, size);
    }
    
    public static Collection<Geometry> getRandomCachedPolygonsLayer2(String id, int size) throws IOException, ParseException {
        return getRandomPolygons(layer2CacheFolder, layer2Name, id, size);
    }
    
    public static Geometry getRandomCachedLayer1ProbabilisticGeometry(String id) throws IOException, ParseException {
        return getRandomCachedPolygonsLayer1(id, 1).iterator().next();
    }
    
    public static Geometry getRandomCachedLayer2ProbabilisticGeometry(String id) throws IOException, ParseException {
        return getRandomCachedPolygonsLayer2(id, 1).iterator().next();
    }
    
    private static Collection<Geometry> getRandomPolygons(String folderPath, String layer, String id, int size) throws IOException, ParseException {
        String cacheId = layer +id;
        
        Map<Integer, Geometry> result = new HashMap<Integer, Geometry>();
        
        Set<Integer> randomNumbers = new HashSet<Integer>();
        Random random = new Random();
        
        while (randomNumbers.size() < size) 
            randomNumbers.add(random.nextInt(NUM_CACHE_GEOMETRIES));
        
        Map<Integer, Geometry> geometries = null;
        
        geometries = randomCache.get(cacheId);
        if(geometries != null) {
            for (Integer randomNum: randomNumbers) {
                Geometry geom = geometries.get(randomNum);
                
                if (geom != null) {
                    result.put(randomNum, geom);
                }
            }
        }
        
        if(geometries == null) {
            geometries = new HashMap<Integer, Geometry>();
            randomCache.put(cacheId, geometries);
        }
        
        BufferedReader reader = new BufferedReader(new FileReader(folderPath +"/"+id));
        for(int i = 0; i < NUM_CACHE_GEOMETRIES; i++) {
            if (result.size() == size)
                break;
            
            if (!randomNumbers.contains(i) || result.containsKey(i)) {
                reader.readLine();
                continue;
            }
            
            String line = reader.readLine();
            Geometry geom = JtsFactories.readWKT(line);
            geometries.put(i, geom);
            result.put(i, geom);
        }
        reader.close();
        
        return result.values();
    }
}
