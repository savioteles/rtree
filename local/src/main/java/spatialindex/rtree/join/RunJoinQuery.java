package spatialindex.rtree.join;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

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
import com.vividsolutions.jts.io.ParseException;

public class RunJoinQuery {

    private static PropertiesReader properties;
    private static RTreeIRTree treeLayer1;
    private static RTreeIRTree treeLayer2;
    
	public static void main(String[] args) throws Exception {
	    
	    if(args.length != 2) {
	        System.err.println("Erro na passagem de parametros. Execute o programa passando: "
	                + "[caminho para arquivo de propriedades] [opção númerica(0, 1 ou 2) do join: "
	                + "0 - Welder Join\n"
	                + "1 - RSJoin"
	                + "2 - Verdade\n]");
	        
	        System.exit(1);
	    }
	    String configurationFilePath = args[0];
        properties = PropertiesReader.getInstance(configurationFilePath);
        
        System.out.println("Iniciando o join...");
        int option = Integer.parseInt(args[1]);
        switch (option) {
        case 0:
            runWelderJoin();
            break;
        case 1:
            runRSJoin();
            break;
        case 2:
            runGroundTruthJoin();
            break;
        default:
            break;
        }
       
        System.out.println("FINISHED");
	}
	
	@SuppressWarnings("deprecation")
    private static void runGroundTruthJoin() throws IllegalArgumentException, NoSuchElementException, IOException, ParseException {
        String filePathLayer1 = properties.getLayer1Path();
        String filePathLayer2 = properties.getLayer2Path();
        int numCacheGeometries = PropertiesReader.getInstance().getNumCacheGeometries();
        
        long time = System.currentTimeMillis();
        ShapefileDataStore shpLayer1 = new ShapefileDataStore(new File(filePathLayer1).toURL());
        ShapefileDataStore shpLayer2 = new ShapefileDataStore(new File(filePathLayer2).toURL());
        List<JoinResultPair> joinRtrees = new GroundTruthJoin().runJoin(shpLayer1, shpLayer2, numCacheGeometries);
        
        String joinResultEntriesFilePath = properties.getResultJoinFilePath() 
                +"_ground_truth_" +numCacheGeometries +".txt";
        writeResult(joinRtrees, joinResultEntriesFilePath);
        
        System.out.println("Tempo Ground Truth: " +(System.currentTimeMillis() - time) +"\t" +joinRtrees.size());
	}
	
	
	private static void runRSJoin() throws IOException, NodeIsLeafException{
	    buildRtrees();
        List<Integer> numJoinIterations = PropertiesReader.getInstance().getNumJoinIterations();
        int numJoinExecutions = PropertiesReader.getInstance().getNumJoinExecutions();
        for(int numJoinIteration: numJoinIterations) {
            String joinResultTimeFilePath = properties.getResultJoinFilePath()
                    +"_rsjoin_maxit" +numJoinIteration +"_time.txt" ;
            BufferedWriter bw = new BufferedWriter(new FileWriter(joinResultTimeFilePath));
            
            long totalTime = 0;
            List<JoinResultPair> joinRtrees = null;
            for(int i = 0; i < numJoinExecutions; i++) {
                long time = System.currentTimeMillis();
                joinRtrees = new RSJoinQuery(numJoinIteration).joinRtrees(treeLayer1, treeLayer2);
                time = System.currentTimeMillis() - time;
                totalTime += time;
                bw.write("Join " +i +": " +time +"\n");
            }
            System.out.println("Total Time: " +totalTime 
                    +". Avg Time: " +(totalTime/numJoinExecutions) 
                    +". Size: " +joinRtrees.size() +". Num join iteration: " +numJoinIteration);
            bw.close();
            
            String joinResultEntriesFilePath = properties.getResultJoinFilePath() 
                    +"_rsjoin_maxit" +numJoinIteration +".txt" ;
            writeResult(joinRtrees, joinResultEntriesFilePath);
        }
    }
	
	private static void runWelderJoin() throws IOException, NodeIsLeafException{
	    buildRtrees();
	    List<Integer> numJoinIterations = PropertiesReader.getInstance().getNumJoinIterations();
        List<Double> gammaValues = PropertiesReader.getInstance().getGammaValues();
	    int numJoinExecutions = PropertiesReader.getInstance().getNumJoinExecutions();
        for(int numJoinIteration: numJoinIterations) {
            for(double gammaValue: gammaValues) {
                
                String joinResultTimeFilePath = properties.getResultJoinFilePath()
                        +"_gamma" +gammaValue +"_maxit" +numJoinIteration +"_time.txt" ;
                BufferedWriter bw = new BufferedWriter(new FileWriter(joinResultTimeFilePath));
                
                long totalTime = 0;
                List<JoinResultPair> joinRtrees = null;
                for(int i = 0; i < numJoinExecutions; i++) {
                    long time = System.currentTimeMillis();
                    joinRtrees = new WelderJoinQuery(numJoinIteration, gammaValue).joinRtrees(treeLayer1, treeLayer2);
                    time = System.currentTimeMillis() - time;
                    totalTime += time;
                    bw.write("Join " +i +": " +time +"\n");
                }
                System.out.println("Total Time: " +totalTime 
                        +". Avg Time: " +(totalTime/numJoinExecutions) 
                        +". Size: " +joinRtrees.size() +". Num join iteration: " +numJoinIteration
                        +". Gamma: " +gammaValue);
                bw.close();
                
                String joinResultEntriesFilePath = properties.getResultJoinFilePath() 
                        +"_gamma" +gammaValue +"_maxit" +numJoinIteration +".txt" ;
                writeResult(joinRtrees, joinResultEntriesFilePath);
            }
        }
	}
	
	private static void buildRtrees() throws IOException, NodeIsLeafException {
	    String layerName1 = properties.getLayer1Name();
        String filePathLayer1 = properties.getLayer1Path();
        int capacityLayer1 = properties.getLayer1Capacity();
        treeLayer1 = buildRtree(layerName1, filePathLayer1, capacityLayer1);
        
        String layerName2 = properties.getLayer2Name();
        String filePathLayer2 = properties.getLayer2Path();
        int capacityLayer2 = properties.getLayer2Capacity();
        treeLayer2 = buildRtree(layerName2, filePathLayer2, capacityLayer2);
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
	
	private static void writeResult(List<JoinResultPair> result, String resultJoinFilePath) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(resultJoinFilePath));
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
