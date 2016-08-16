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

import com.vividsolutions.jts.geom.Geometry;

public class BuildProbabilisticPolygons {

	private static ThreadPoolExecutor pool = new ThreadPoolExecutor(8, 12, 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
	private static final int NUM_POLYGONS = 1000;

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		long time = System.currentTimeMillis();
		ShapefileDataStore shp = new ShapefileDataStore(
				new File(
						"/media/savio/Dados/Shapes/LAPIG/Paises/pa_br_vegetacao_71.shp")
						.toURL());
//		write(shp,
//				"/media/savio/Dados/Faculdade/Artigos/Geoinfo_2016 Welder/polygons/vegeta");
//		System.out.println("Time to construct vegeta polygons: "
//				+ (System.currentTimeMillis() - time));

		time = System.currentTimeMillis();
		shp = new ShapefileDataStore(
				new File(
						"/media/savio/Dados/Shapes/LAPIG/Biomas/bi_ce_alertas_desmatamento_1.shp")
						.toURL());
		write(shp,
				"/media/savio/Dados/Faculdade/Artigos/Geoinfo_2016 Welder/polygons/desmata");
		System.out.println("Time to construct desmata polygons: "
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
				BufferedWriter bw = new BufferedWriter(new FileWriter(outPath
						+ "/" + id));
				Geometry originalGeom = getGeomOfFeature(feature, featureType);

				for (int i = 0; i < NUM_POLYGONS; i++) {
					Geometry geom = JtsFactories
							.changeGeometryPointsProbabilistic(originalGeom, 50);
					if (!geom.isValid()) {
						geom = originalGeom.convexHull();
						geom = JtsFactories.changeGeometryPointsProbabilistic(
								geom, 50);
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
