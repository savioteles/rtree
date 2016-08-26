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

    private static int numThreads = PropertiesReader.getInstance().getNumSystemThreads();
	private static ThreadPoolExecutor pool = new ThreadPoolExecutor(numThreads, numThreads * 2, 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
	private static final int NUM_POLYGONS = PropertiesReader.getInstance().getNumCacheGeometries();
	private static final int errorInMeters = PropertiesReader.getInstance().getErrorInMeters();

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		long time = System.currentTimeMillis();
		ShapefileDataStore shp = new ShapefileDataStore(
				new File(
						PropertiesReader.getInstance().getLayer1Path())
						.toURL());
		write(shp,
		        PropertiesReader.getInstance().getCacheGeometrieslayer1Path());
		System.out.println("Time to construct layer 1 polygons: "
				+ (System.currentTimeMillis() - time));

		time = System.currentTimeMillis();
		shp = new ShapefileDataStore(
				new File(
				        PropertiesReader.getInstance().getLayer2Path())
						.toURL());
		write(shp,
		        PropertiesReader.getInstance().getCacheGeometrieslayer2Path());
		System.out.println("Time to construct layer 2 polygons: "
				+ (System.currentTimeMillis() - time));

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
