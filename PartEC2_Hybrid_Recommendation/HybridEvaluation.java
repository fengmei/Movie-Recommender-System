package com.RecommenderSystem;

import java.io.*; import java.util.*;
import org.apache.mahout.cf.taste.common.TasteException; 
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator; 
import org.apache.mahout.cf.taste.impl.eval.GenericRecommenderIRStatsEvaluator; 
import org.apache.mahout.cf.taste.impl.eval.RMSRecommenderEvaluator; 
import org.apache.mahout.cf.taste.impl.model.file.*; 
import org.apache.mahout.cf.taste.impl.neighborhood.*; 
import org.apache.mahout.cf.taste.impl.recommender.*; 
import org.apache.mahout.cf.taste.impl.similarity.*; 
import org.apache.mahout.cf.taste.model.*; 
import org.apache.mahout.cf.taste.neighborhood.*; 
import org.apache.mahout.cf.taste.recommender.*; 
import org.apache.mahout.cf.taste.similarity.*; 
import org.apache.mahout.cf.taste.eval.*; 
import org.apache.mahout.common.RandomUtils;


public class HybridEvaluation {
public static void main(String[] args) throws IOException {
DataModel model = new FileDataModel(new
File("/Users/houwe/workspace/RecommenderSystem/hybriddatasetN.csv "));
// Use precision/Recall evaluation 
RecommenderEvaluator rmse = new RMSRecommenderEvaluator();
RecommenderIRStatsEvaluator IRevaluator = new GenericRecommenderIRStatsEvaluator();
RecommenderBuilder recommenderBuilder = new RecommenderBuilder() { 
	public Recommender buildRecommender(DataModel model) throws TasteException {
ItemSimilarity similarity = new PearsonCorrelationSimilarity(model);
return new GenericItemBasedRecommender(model,similarity);
} 
	};
	
double rmseScore = 0.0;
IRStatistics stats = null; 
try {
rmseScore = rmse.evaluate(recommenderBuilder, null, model,0.7, 1.0);
stats = IRevaluator.evaluate(recommenderBuilder, null, model, null, 5, GenericRecommenderIRStatsEvaluator.CHOOSE_THRESHOLD, 1);
}
catch (TasteException e) { 
	e.printStackTrace(); 
	} 
System.out.println("The evaluation based on RMSE for Item based recommendation is: "  + rmseScore);
System.out.println(stats);
}
}