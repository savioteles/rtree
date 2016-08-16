package spatialindex.rtree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.util.LRULinkedHashMap;

import utils.JtsFactories;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public class PolygonCachingService {
    private static final String desmataCacheFolder = "/home/savio/polygons/desmata";
    private static final String vegetaCacheFolder = "/home/savio/polygons/vegeta";
    
    private static final int MAX_CACHE_SIZE = 10000;
    
    private static LRULinkedHashMap<String, List<Geometry>> cache = new LRULinkedHashMap<String, List<Geometry>>(MAX_CACHE_SIZE, 0.75f, false, MAX_CACHE_SIZE);
    
    public static List<Geometry> getDesmataPolygons(String id, int size) throws IOException, ParseException {
        return getPolygons(desmataCacheFolder, "desmata", id, size);
    }
    
    public static List<Geometry> getVegetaPolygons(String id, int size) throws IOException, ParseException {
        return getPolygons(vegetaCacheFolder, "vegeta", id, size);
    }
    
    public static List<Geometry> getPolygons(String folderPath, String layer, String id, int size) throws IOException, ParseException {
        String cacheId = layer +id;
        List<Geometry> geometries = cache.get(cacheId);
        
        if(geometries != null && geometries.size() >= size)
            return geometries;
        
        if(geometries == null) {
            geometries = new ArrayList<Geometry>();
        }
        
        BufferedReader reader = new BufferedReader(new FileReader(folderPath +"/"+id));
        
        for(int i = geometries.size(); i < size; i++) {
            String line = reader.readLine();
            Geometry geom = JtsFactories.readWKT(line);
            geometries.add(geom);
        }
        
        cache.put(cacheId, geometries);
        reader.close();
        
        return geometries;
    } 
}
