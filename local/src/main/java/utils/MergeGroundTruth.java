package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.util.Pair;

public class MergeGroundTruth {

    public static void main(String[] args) throws IOException {
        String fileDir = "/home/savio/polygons/desmata_queimada/reresultadoprobabilisticjoin";
        File dir = new File(fileDir);
        
        Map<String, Pair<Integer, Integer>> chiSquareMap = new HashMap<String, Pair<Integer,Integer>>();
        Map<String, Pair<Integer, Integer>> exponentialMap = new HashMap<String, Pair<Integer,Integer>>();
        Map<String, Pair<Integer, Integer>> normalMap = new HashMap<String, Pair<Integer,Integer>>();
        
        for(File localDir: dir.listFiles()) {
            String chiSquaredFile = localDir.getPath() +"/result_rsjoin_chisquared_ground_truth_150.txt";
            BufferedReader brChi = new BufferedReader(new FileReader(chiSquaredFile));
            String line = null;
            while((line = brChi.readLine()) != null) {
                String[] split = line.split(";");
                String key = split[0] +";" +split[1];
                int falseIntersections = Integer.parseInt(split[2]);
                int trueIntersections = Integer.parseInt(split[3]);
                Pair<Integer, Integer> pair = chiSquareMap.get(key);
                if(pair == null) 
                    pair = new Pair<Integer, Integer>(0, 0);
                
                falseIntersections += pair.getKey();
                trueIntersections += pair.getValue();
                
                chiSquareMap.put(key, new Pair<Integer, Integer>(falseIntersections, trueIntersections));
            }
            brChi.close();
            
            String exponencialFile = localDir.getPath() +"/result_rsjoin_exponencial_ground_truth_150.txt";
            BufferedReader brExp = new BufferedReader(new FileReader(exponencialFile));
            while((line = brExp.readLine()) != null) {
                String[] split = line.split(";");
                String key = split[0] +";" +split[1];
                int falseIntersections = Integer.parseInt(split[2]);
                int trueIntersections = Integer.parseInt(split[3]);
                Pair<Integer, Integer> pair = exponentialMap.get(key);
                if(pair == null) 
                    pair = new Pair<Integer, Integer>(0, 0);
                
                falseIntersections += pair.getKey();
                trueIntersections += pair.getValue();
                
                exponentialMap.put(key, new Pair<Integer, Integer>(falseIntersections, trueIntersections));
            }
            brExp.close();
            
            String normalFile = localDir.getPath() +"/result_rsjoin_normal_ground_truth_150.txt";
            BufferedReader brNormal = new BufferedReader(new FileReader(normalFile));
            while((line = brNormal.readLine()) != null) {
                String[] split = line.split(";");
                String key = split[0] +";" +split[1];
                int falseIntersections = Integer.parseInt(split[2]);
                int trueIntersections = Integer.parseInt(split[3]);
                Pair<Integer, Integer> pair = normalMap.get(key);
                if(pair == null) 
                    pair = new Pair<Integer, Integer>(0, 0);
                
                falseIntersections += pair.getKey();
                trueIntersections += pair.getValue();
                
                normalMap.put(key, new Pair<Integer, Integer>(falseIntersections, trueIntersections));
            }
            brNormal.close();
        }
        
        write(chiSquareMap, fileDir +"/result_rsjoin_chisquared_ground_truth.txt");
        write(exponentialMap, fileDir +"/result_rsjoin_exponencial_ground_truth.txt");
        write(normalMap, fileDir +"/result_rsjoin_normal_ground_truth.txt");
    }
    
    private static void write(Map<String, Pair<Integer, Integer>> map, String filePath) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
        for(String key: map.keySet()) {
            Pair<Integer, Integer> pair = map.get(key);
            int trueIntersections = pair.getValue();
            int falseIntersections = 900 - trueIntersections;
            bw.write(key +";" +falseIntersections +";" +trueIntersections +"\n");
        }
        bw.close();
                
    }
    

}
