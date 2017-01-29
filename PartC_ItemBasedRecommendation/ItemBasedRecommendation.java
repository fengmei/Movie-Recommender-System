package com.Cmpe239Movies;

import java.io.*;
import java.util.*;

import org.apache.mahout.cf.taste.impl.model.file.*;
import org.apache.mahout.cf.taste.impl.recommender.*;
import org.apache.mahout.cf.taste.impl.similarity.*;
import org.apache.mahout.cf.taste.model.*;
import org.apache.mahout.cf.taste.recommender.*;
import org.apache.mahout.cf.taste.similarity.*;

public class ItemBasedRecommendation {

	public static void main(String[] args) {
		try{	
			
			//Step 1:- Load our data file, and the file is in userID, itemID, ratings format
			DataModel model = new FileDataModel(new File("/Users/XC/Dropbox/SJSU/Courses/Cmpe 239 2016 Spring/ProjectA/UpdatedData/UserdatasetN.csv"));

			//Step 2:- Create ItemSimilarity Matrix	: we use Pearson Correlation here	
			ItemSimilarity similarity = new PearsonCorrelationSimilarity(model);
					
			//Step 3:- Create object of ItemBasedRecommender						
			GenericItemBasedRecommender recommender = new GenericItemBasedRecommender(model, similarity);
						
			//Step 4:- Call the Generated Recommender in previous step to getting recommendation for particular User			
			//We choose to recommend each user top 5 movies.
	         List<RecommendedItem> recommendations = recommender.recommend(179 , 5);
	         
	         for (RecommendedItem recommendation : recommendations) {
	            System.out.println(recommendation);
	         }
		}
		catch (Exception e) {
			System.out.println("There was an error.");
			e.printStackTrace();
		}		

	}

}
