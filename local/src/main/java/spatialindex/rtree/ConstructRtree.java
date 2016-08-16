package spatialindex.rtree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.geotools.data.FeatureReader;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

import spatialindex.rtree.RTreeJoinQuery.JoinResultPair;
import utils.JtsFactories;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

public class ConstructRtree {

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		long time = System.currentTimeMillis();
		RTreeNode vegetaRoot = new RTreeNode(200, true, "vegeta_root");
		RTreeIRTree vegetaRtree = new RStar(200, "vegetacao", false);
		ShapefileDataStore shp = new ShapefileDataStore(new File("/media/savio/Dados/Shapes/LAPIG/Paises/pa_br_vegetacao_71.shp").toURL());
		FeatureReader<SimpleFeatureType,SimpleFeature> reader = shp.getFeatureReader();
		FeatureType featureType = reader.getFeatureType();
		
        while(reader.hasNext()) {
        	Feature feature = reader.next();
        	
			Geometry geom = getGeomOfFeature(feature,
                    featureType);
			geom = JtsFactories.changeGeometryPointsProbabilistic(geom, 50);
			if(!geom.isValid()) {
				geom = geom.convexHull();
				geom = JtsFactories.changeGeometryPointsProbabilistic(geom, 50);
			}
			RTreeInsertion.insertTree(vegetaRoot, new RTreeEntryData(geom.getEnvelopeInternal(), new IndexObject(feature.getIdentifier().getID().split("\\.")[1], geom)), null, vegetaRtree);
        }
        
        RTreeNode desmataRoot = new RTreeNode(500, true, "desmata_root");
		RTreeIRTree desmataRtree = new RStar(500, "desmata_root", false);
		shp = new ShapefileDataStore(new File("/media/savio/Dados/Shapes/LAPIG/Biomas/bi_ce_alertas_desmatamento_1.shp").toURL());
		reader = shp.getFeatureReader();
		featureType = reader.getFeatureType();
		
        while(reader.hasNext()) {
        	Feature feature = reader.next();
			Geometry geom = getGeomOfFeature(feature,
                    featureType);
			geom = JtsFactories.changeGeometryPointsProbabilistic(geom, 50);
			if(!geom.isValid()) {
				geom = geom.convexHull();
				geom = JtsFactories.changeGeometryPointsProbabilistic(geom, 50);
			} 
			RTreeInsertion.insertTree(desmataRoot, new RTreeEntryData(geom.getEnvelopeInternal(), new IndexObject(feature.getIdentifier().getID().split("\\.")[1], geom)), null, desmataRtree);
        }
        
        System.out.println("Time to construct: " +(System.currentTimeMillis() - time));
        
        System.out.println("Iniciando o join...");
        time = System.currentTimeMillis();
        List<JoinResultPair> joinRtrees = new RSJoinQuery().joinRtrees(desmataRtree, vegetaRtree);
        System.out.println("Time: " +(System.currentTimeMillis() - time) +". Size: " +joinRtrees.size());
        
        System.out.println("FINISHED");

	}
	
	public static Geometry getGeomOfFeature(Feature f,
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
