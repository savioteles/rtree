package spatialindex.rtree.join;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.geotools.data.FeatureReader;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

import spatialindex.rtree.IndexObject;
import spatialindex.rtree.RStar;
import spatialindex.rtree.RTreeEntryData;
import spatialindex.rtree.RTreeINode.NodeIsLeafException;
import spatialindex.rtree.RTreeIRTree;
import spatialindex.rtree.RTreeInsertion;
import spatialindex.rtree.RTreeNode;
import spatialindex.rtree.join.RTreeJoinQuery.JoinResultPair;
import utils.PropertiesReader;

import com.vividsolutions.jts.geom.Geometry;

public class RunJoinQuery {

    private static PropertiesReader properties;
	public static void main(String[] args) throws Exception {
	    
	    String configurationFilePath = args[0];
        properties = PropertiesReader.getInstance(configurationFilePath);
        
        String layerName1 = properties.getLayer1Name();
	    String filePathLayer1 = properties.getLayer1Path();
	    int capacityLayer1 = properties.getLayer1Capacity();
        RTreeIRTree treeLayer1 = buildRtree(layerName1, filePathLayer1, capacityLayer1);
        
        String layerName2 = properties.getLayer2Name();
        String filePathLayer2 = properties.getLayer2Path();
        int capacityLayer2 = properties.getLayer2Capacity();
        RTreeIRTree treeLayer2 = buildRtree(layerName2, filePathLayer2, capacityLayer2);
        
        System.out.println("Iniciando o join...");
        BufferedWriter bw = new BufferedWriter(new FileWriter(properties.getJoinResultTimeFilePath()));
        List<JoinResultPair> joinRtrees = null;
        long totalTime = 0;
        int numJoinExecutions = PropertiesReader.getInstance().getNumJoinExecutions();
        for(int i = 0; i < numJoinExecutions; i++) {
            long time = System.currentTimeMillis();
            joinRtrees = new RSJoinQuery().joinRtrees(treeLayer1, treeLayer2);
            time = System.currentTimeMillis() - time;
            totalTime += time;
            bw.write("Join " +i +": " +time +"\n");
           
        }
        System.out.println("Total Time: " +totalTime +". Avg Time: " +(totalTime/numJoinExecutions) +". Size: " +joinRtrees.size());
        bw.close();
        
        writeResult(joinRtrees);
        System.out.println("FINISHED");
	}
	
	@SuppressWarnings("deprecation")
    private static RTreeIRTree buildRtree(String layerName, String filePath, int capacity) throws IOException, NodeIsLeafException {
	    long time = System.currentTimeMillis();
        RTreeNode root = new RTreeNode(capacity, true, layerName +"_root");
        RTreeIRTree tree = new RStar(capacity, layerName, false);
        ShapefileDataStore shp = new ShapefileDataStore(new File(filePath).toURL());
        FeatureReader<SimpleFeatureType,SimpleFeature> reader = shp.getFeatureReader();
        FeatureType featureType = reader.getFeatureType();
        
        while(reader.hasNext()) {
            Feature feature = reader.next();
       
            Geometry geom = getGeomOfFeature(feature,
                    featureType);
            RTreeInsertion.insertTree(root, new RTreeEntryData(geom.getEnvelopeInternal(), new IndexObject(feature.getIdentifier().getID().split("\\.")[1], geom)), null, tree);
        }
        
        System.out.println("Time to construct layer " +layerName +": " +(System.currentTimeMillis() - time));
        
        return tree;
	}
	
	private static void writeResult(List<JoinResultPair> result) throws IOException {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(properties.getResultJoinFilePath()));
	    for(JoinResultPair r: result) {
	        if(r == null)
	            continue;
	        bw.write(r.left +";" +r.right +";" +r.false_intersections +";" +r.true_intersections +"\n");
	    }
	    
	    bw.close();
	}
	
	private static Geometry getGeomOfFeature(Feature f,
            FeatureType featureType) {
        for (Property prop : f.getProperties())
            if (prop.getName().getURI().toLowerCase().intern()
                    .equals(featureType.getGeometryDescriptor().getName()
                            .toString())) {
                Geometry geometry = (Geometry) prop.getValue();
                return geometry;
            }

        return null;
    }

}
