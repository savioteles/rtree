package spatialindex.rtree.join;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.util.LRULinkedHashMap;

import utils.JtsFactories;
import utils.PropertiesReader;

import com.google.common.base.Strings;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public class ProbabilisticGeometriesService {
    private static final String desmataCacheFolder = PropertiesReader.getInstance().getCacheGeometrieslayer1Path();
    private static final String vegetaCacheFolder = PropertiesReader.getInstance().getCacheGeometrieslayer2Path();
    
    private static final int MAX_CACHE_SIZE = PropertiesReader.getInstance().getMaxCacheEntries();
    private static final int ERROR_METERS = PropertiesReader.getInstance().getErrorInMeters();
    
    private static LRULinkedHashMap<String, List<Geometry>> cache = new LRULinkedHashMap<String, List<Geometry>>(MAX_CACHE_SIZE, 0.75f, false, MAX_CACHE_SIZE);
    
    public static Geometry getProbabilisticDesmataGeometry(Geometry originalGeom, String id, int index) throws IOException, ParseException {
        return getProbabilisticGeometry(originalGeom, "desmata", id, index);
    }
    
    public static Geometry getProbabilisticVegetaGeometry(Geometry originalGeom, String id, int index) throws IOException, ParseException {
        return getProbabilisticGeometry(originalGeom, "vegeta", id, index);
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
                probabilisticGeom = originalGeom.convexHull();
                probabilisticGeom = JtsFactories.changeGeometryPointsProbabilistic(
                        probabilisticGeom, ERROR_METERS);
            }
            geometries.add(probabilisticGeom);
            
            return probabilisticGeom;
        }
    }
    
    public static List<Geometry> getCachedDesmataPolygons(String id, Geometry geom, int size) throws IOException, ParseException {
        return getPolygons(desmataCacheFolder, "desmata", geom, id, size, true);
    }
    
    public static List<Geometry> getDesmataPolygons(String id, Geometry geom, int size) throws IOException, ParseException {
        return getPolygons(desmataCacheFolder, "desmata", geom, id, size, false);
    }
    
    public static List<Geometry> getCachedVegetaPolygons(String id, Geometry geom, int size) throws IOException, ParseException {
        return getPolygons(vegetaCacheFolder, "vegeta", geom, id, size, true);
    }
    
    public static List<Geometry> getVegetaPolygons(String id, Geometry geom, int size) throws IOException, ParseException {
        return getPolygons(vegetaCacheFolder, "vegeta", geom, id, size, false);
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
}
