package com.Cmpe239Movies;

import java.io.*;

import org.apache.mahout.cf.taste.common.TasteException;
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

public class UserBasedEvaluation {

	public static void main(String[] args) throws IOException {
		RandomUtils.useTestSeed();
		
		//Step 1:- Input our data file, and the file is in userID, itemID, ratings format
		DataModel model = new FileDataModel(new File("/Users/XC/Dropbox/SJSU/Courses/Cmpe 239 2016 Spring/ProjectA/UpdatedData/UserdatasetN.csv"));
		
		// We would like to use two kinds of methods to evaluate our results, which are 
		// Root-mean-square error (RMSE) and Precision.
		RecommenderEvaluator rmse = new RMSRecommenderEvaluator();
		RecommenderIRStatsEvaluator IRevaluator = new GenericRecommenderIRStatsEvaluator();
				
		// Build the same recommender for testing that we did last time:
		RecommenderBuilder recommenderBuilder = new RecommenderBuilder() {
			public Recommender buildRecommender(DataModel model) throws TasteException {
				UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
				UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);

				return new GenericUserBasedRecommender(model, neighborhood, similarity);
		}
		};
		
		double rmseScore = 0.0;
		IRStatistics stats = null;
		
		try {
			// Consider all the dataset in this step, and divided dataset into traing set and testing set
			// 70% of the data are for training, and 30% of the data are for testing
			rmseScore = rmse.evaluate(recommenderBuilder, null, model, 0.7, 1.0);
			stats = IRevaluator.evaluate(recommenderBuilder, null, model, null, 5, GenericRecommenderIRStatsEvaluator.CHOOSE_THRESHOLD, 1);
		} catch (TasteException e) {
			e.printStackTrace();
		}
		
		System.out.println("The evaluation based on RMSE for User based recommendation is: "  + rmseScore);
		System.out.println(stats);		
		}

}
