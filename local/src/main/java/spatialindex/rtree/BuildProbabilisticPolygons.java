package spatialindex.rtree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

import utils.JtsFactories;
import utils.PropertiesReader;

import com.vividsolutions.jts.geom.Geometry;

public class BuildProbabilisticPolygons {

    private static int numThreads;
	private static ThreadPoolExecutor pool;
	private static int NUM_POLYGONS;
	private static int errorInMeters;

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
	    if(args.length != 2) {
            System.err.println("Erro na passagem de parametros. Execute o programa passando: "
                    + "[caminho para arquivo de propriedades] [opção númerica(0, 1 ou 2) do construtor de geometrias: \n"
                    + "0 - Construir as geometrias probabilisticas apenas do layer 1\n"
                    + "1 - Construir as geometrias probabilisticas apenas do layer 2\n"
                    + "2 - Construir as geometrias probabilisticas apenas de ambos os layers.]");
            
            System.exit(1);
        }
	    
	    String configurationFilePath = args[0];
	    PropertiesReader instance = PropertiesReader.getInstance(configurationFilePath);
	    
	    
        numThreads = instance.getNumSystemThreads();
	    pool = new ThreadPoolExecutor(numThreads, numThreads * 2, 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
	    NUM_POLYGONS = instance.getNumCacheGeometries();
	    errorInMeters = instance.getErrorInMeters();
	    
	    int option = Integer.parseInt(args[1]);
	    
	    if(option == 0 || option == 2) {
    		long time = System.currentTimeMillis();
    		ShapefileDataStore shp = new ShapefileDataStore(
    				new File(
    						instance.getLayer1Path())
    						.toURL());
    		write(shp,
    		        instance.getCacheGeometrieslayer1Path());
    		System.out.println("Time to construct layer 1 polygons: "
    				+ (System.currentTimeMillis() - time));
	    }

	    if(option == 1 || option == 2) {
    		long time = System.currentTimeMillis();
    		ShapefileDataStore shp = new ShapefileDataStore(
    				new File(
    				        instance.getLayer2Path())
    						.toURL());
    		write(shp,
    		        instance.getCacheGeometrieslayer2Path());
    		System.out.println("Time to construct layer 2 polygons: "
    				+ (System.currentTimeMillis() - time));
	    }

	}

	private static void write(ShapefileDataStore shp, String outPath)
			throws IOException, InterruptedException {
		FeatureReader<SimpleFeatureType, SimpleFeature> reader = shp
				.getFeatureReader();
		FeatureType featureType = reader.getFeatureType();

		while (reader.hasNext()) {
			Feature feature = reader.next();
			pool.execute(new ThreadWriter(outPath, featureType, feature));
		}
		
		pool.shutdown();
		pool.awaitTermination(1, TimeUnit.DAYS);
		pool = new ThreadPoolExecutor(8, 12, 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
	}

	private static class ThreadWriter implements Runnable {

		private String outPath;
		private FeatureType featureType;
		private Feature feature;

		public ThreadWriter(String outPath,
				FeatureType featureType, Feature feature) {
			this.outPath = outPath;
			this.featureType = featureType;
			this.feature = feature;
		}

		@Override
		public void run() {
			try {
				String id = feature.getIdentifier().getID().split("\\.")[1];
				File dir = new File(outPath);
				if(!dir.exists())
				    dir.mkdirs();
				
                BufferedWriter bw = new BufferedWriter(new FileWriter(outPath
                        + "/" + id));
				Geometry originalGeom = getGeomOfFeature(feature, featureType);

				for (int i = 0; i < NUM_POLYGONS; i++) {
					Geometry geom = JtsFactories
							.changeGeometryPointsProbabilistic(originalGeom, errorInMeters);
					if (!geom.isValid()) {
						geom = originalGeom.convexHull();
						geom = JtsFactories.changeGeometryPointsProbabilistic(
								geom, errorInMeters);
					}

					bw.write(geom.toString() + "\n");
				}

				bw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	public static Geometry getGeomOfFeature(Feature f, FeatureType featureType) {
		for (Property prop : f.getProperties())
			if (prop.getName()
					.getURI()
					.toLowerCase()
					.intern()
					.equals(featureType.getGeometryDescriptor().getName()
							.toString())) {
				Geometry geometry = (Geometry) prop.getValue();
				return geometry;
			}

		return null;
	}

}
