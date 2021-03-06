package utils;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import utils.JtsFactories.Distribution;



public class PropertiesReader {
    
    Properties pro = new Properties();
    private static String configurationFileName;
    
    private static PropertiesReader srv = null;
    public static PropertiesReader getInstance(String configurationFileName) {
        PropertiesReader.configurationFileName = configurationFileName;
        if (srv == null)
            srv = new PropertiesReader();

        return srv;
    }
    
    public static PropertiesReader getInstance() {
        return srv;
    }
    
    private Distribution distribution;
    
    private String layer1Path;
    private String layer2Path;
    
    private String cacheGeometrieslayer1Path;
    private String cacheGeometrieslayer2Path;
    private int numCacheGeometries;
    private int maxCacheEntries;
    
    private int numSystemThreads;
    
    private int errorInMeters;
    private int errorInMetersLayer1;
    private int errorInMetersLayer2;
    private double sd;
    
    private String layer1Name;
    private int layer1Capacity;
    
    private String layer2Name;
    private int layer2Capacity;
    
    private List<Integer> numJoinIterations = new ArrayList<Integer>();
    private String resultJoinFilePath;
    private String joinResultTimeFilePath;
    private int numJoinExecutions;
    private int numJoinIterationsByStep;
    
    private List<Double> gammaValues = new ArrayList<Double>();
    private double p;
    
    protected PropertiesReader() {

        try {
            pro.load(new FileInputStream(configurationFileName));
        } catch (Exception e) {
           e.printStackTrace();
        }

        if (pro.isEmpty())
            System.err.println("File " + configurationFileName + " is empty.");
        
        String distributionName = pro.getProperty("distribution");
        distribution = Distribution.valueOf(distributionName);
        
        layer1Path = pro.getProperty("layer1_path");
        layer2Path = pro.getProperty("layer2_path");
        
        cacheGeometrieslayer1Path = pro.getProperty("cache_layer1_geometries_path");
        cacheGeometrieslayer2Path = pro.getProperty("cache_layer2_geometries_path");
        
        numCacheGeometries = Integer.parseInt(pro.getProperty("num_cache_geometries"));
        maxCacheEntries = Integer.parseInt(pro.getProperty("max_cache_entries"));
        
        numSystemThreads = Integer.parseInt(pro.getProperty("num_threads_system"));
        
        errorInMeters = Integer.parseInt(pro.getProperty("error_in_meters"));
        errorInMetersLayer1 = Integer.parseInt(pro.getProperty("error_in_meters_layer1"));
        errorInMetersLayer2 = Integer.parseInt(pro.getProperty("error_in_meters_layer2"));
        
        String joinIterations = pro.getProperty("num_join_iteratios");
        for(String s: joinIterations.split(",")) {
            numJoinIterations.add(Integer.parseInt(s));
        }
        
        layer1Name = pro.getProperty("layer1_name");
        layer1Capacity = Integer.parseInt(pro.getProperty("layer1_capacity"));
        
        layer2Name = pro.getProperty("layer2_name");
        layer2Capacity = Integer.parseInt(pro.getProperty("layer2_capacity"));
        
        resultJoinFilePath = pro.getProperty("result_join_file_path");
        joinResultTimeFilePath = pro.getProperty("join_process_time_file_path");
        
        numJoinExecutions = Integer.parseInt(pro.getProperty("num_join_executions"));
        numJoinIterationsByStep = Integer.parseInt(pro.getProperty("num_join_iterations_by_step"));
        
        String gammaStr = pro.getProperty("gamma");
        for(String s: gammaStr.split(",")) {
            gammaValues.add(Double.parseDouble(s));
        }
        
        p = Double.parseDouble(pro.getProperty("p"));
        sd = Double.parseDouble(pro.getProperty("sd", "0"));
    }

    public Distribution getDistribution() {
        return distribution;
    }

    public String getLayer1Path() {
        return layer1Path;
    }

    public String getLayer2Path() {
        return layer2Path;
    }

    public String getCacheGeometrieslayer1Path() {
        return cacheGeometrieslayer1Path;
    }

    public String getCacheGeometrieslayer2Path() {
        return cacheGeometrieslayer2Path;
    }

    public int getNumCacheGeometries() {
        return numCacheGeometries;
    }
    
    public int getNumSystemThreads() {
        return numSystemThreads;
    }

    public int getErrorInMeters() {
        return errorInMeters;
    }
    
    public int getErrorInMetersLayer1() {
        return errorInMetersLayer1;
    }

    public int getErrorInMetersLayer2() {
        return errorInMetersLayer2;
    }

    public int getMaxCacheEntries() {
        return maxCacheEntries;
    }

    public List<Integer> getNumJoinIterations() {
        return numJoinIterations;
    }

    public String getLayer1Name() {
        return layer1Name;
    }

    public String getLayer2Name() {
        return layer2Name;
    }

    public int getLayer1Capacity() {
        return layer1Capacity;
    }

    public int getLayer2Capacity() {
        return layer2Capacity;
    }

    public String getResultJoinFilePath() {
        return resultJoinFilePath;
    }

    public String getJoinResultTimeFilePath() {
        return joinResultTimeFilePath;
    }

    public int getNumJoinExecutions() {
        return numJoinExecutions;
    }

    public int getNumJoinIterationsByStep() {
        return numJoinIterationsByStep;
    }

    public List<Double> getGammaValues() {
        return gammaValues;
    }

    public double getP() {
        return p;
    }
    
    public double getSd() {
    	return sd;
    }
}
