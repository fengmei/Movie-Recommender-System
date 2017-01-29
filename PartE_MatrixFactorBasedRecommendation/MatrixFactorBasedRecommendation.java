package com.RecommenderSystem;

import org.apache.spark.api.java.*;
import org.apache.spark.*;
import org.apache.spark.mllib.recommendation.Rating;
import java.util.*;
import scala.Tuple2;
import org.apache.spark.api.java.function.*;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.ALS;

public class MatrixFactorBasedRecommendation {
	static SparkConf conf;
    static JavaSparkContext sc;
    
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//Initializing Spark
        conf = new SparkConf().setAppName("MovieRecommendation").setMaster("local");
        sc = new JavaSparkContext(conf);
        
        //Reading Data
final JavaRDD<String> ratingData = sc.textFile("/Users/houwe/workspace/RecommenderSystem/ratings.csv");
JavaRDD<String> productData = sc.textFile("/Users/houwe/workspace/RecommenderSystem/movies.csv");


  //Ratings file should be csv file in a (UserID, MovieID, Rating,timestamp) Format
  // assign some random Integer in timestamp filed (timestamp will be used to divide data in training, test and validation dataset)
  //Keep this block as it is
JavaRDD<Tuple2<Integer, Rating>> ratings = ratingData.map(
        new Function<String, Tuple2<Integer, Rating>>() {
            public Tuple2<Integer, Rating> call(String s) throws Exception {
                String[] row = s.split(",");
                Integer cacheStamp = Integer.parseInt(row[3]) % 10;
                Rating rating = new Rating(Integer.parseInt(row[0]), Integer.parseInt(row[1]), Double.parseDouble(row[2]));
                return new Tuple2<Integer, Rating>(cacheStamp, rating);
            }
        }
);

//Movies file should be csv file in a (MovieID,Title) format
//Keep this block as it is
Map<Integer, String> products = productData.mapToPair(
        new PairFunction<String, Integer, String>() {
            public Tuple2<Integer, String> call(String s) throws Exception {
                String[] sarray = s.split(",");
                return new Tuple2<Integer, String>(Integer.parseInt(sarray[0]), sarray[1]);
            }
        }
).collectAsMap();


//training data set
// below function generate training data from input data 
// keep other things as it is
JavaRDD<Rating> training = ratings.filter(
        new Function<Tuple2<Integer, Rating>, Boolean>() {
            public Boolean call(Tuple2<Integer, Rating> tuple) throws Exception {
                return tuple._1() < 6;
                // write your logic to create training data set based on timestamp from input dataset
            }
        }
).map(
        new Function<Tuple2<Integer, Rating>, Rating>() {
            public Rating call(Tuple2<Integer, Rating> tuple) throws Exception {
                return tuple._2();
            }
        }
).cache();


//validation data set
// below function generate validation data from input data 
// keep other things as it is
JavaRDD<Rating> validation = ratings.filter(
        new Function<Tuple2<Integer, Rating>, Boolean>() {
            public Boolean call(Tuple2<Integer, Rating> tuple) throws Exception {
                return tuple._1() >= 6 && tuple._1() < 8;
                
            }
        }
).map(
        new Function<Tuple2<Integer, Rating>, Rating>() {
            public Rating call(Tuple2<Integer, Rating> tuple) throws Exception {
                return tuple._2();
            }
        }
);

//test data set
// below function generate validation data from input data 
// keep other things as it is
JavaRDD<Rating> test = ratings.filter(
        new Function<Tuple2<Integer, Rating>, Boolean>() {
            public Boolean call(Tuple2<Integer, Rating> tuple) throws Exception {
                return tuple._1() >= 8;
               
            }
        }
).map(
        new Function<Tuple2<Integer, Rating>, Rating>() {
            public Rating call(Tuple2<Integer, Rating> tuple) throws Exception {
                return tuple._2();
            }
        }
);



//train the model with sparks ALS.train method
// try different value of rank, lambda and numofIterations and test accuracy of model everytime
// to pick the model with highest accuracy

// To calculate accuracy, write your logic in computeAccuracy model provided below


//After creating tarining model call getRecommendation model and print recommendationd for particular user
// getRecommendation model will take 4 input paramaeters (userId, trained model,All user Ratings, All Movies)
// Implement Get recommendation method below

MatrixFactorizationModel model = ALS.train(JavaRDD.toRDD(training), 10, 10, 0.05);
computeAccuracy(model, validation); 
System.out.println(getRecommendations(792, model, ratings, products));

}

public static Double computeAccuracy(MatrixFactorizationModel model, JavaRDD<Rating> validation) {

    Double answer =1.0;

     //Write your logic of calculating accuracy of trained model here

    JavaRDD<Tuple2<Object, Object>> userProducts = validation.map(

       new Function<Rating, Tuple2<Object, Object>>() {

         public Tuple2<Object, Object> call(Rating r) {

           return new Tuple2<Object, Object>(r.user(), r.product());

         }

       }

     );

     JavaPairRDD<Tuple2<Integer, Integer>, Double> predictions = JavaPairRDD.fromJavaRDD(

       model.predict(JavaRDD.toRDD(userProducts)).toJavaRDD().map(

         new Function<Rating, Tuple2<Tuple2<Integer, Integer>, Double>>() {

           public Tuple2<Tuple2<Integer, Integer>, Double> call(Rating r){

             return new Tuple2<Tuple2<Integer, Integer>, Double>(

               new Tuple2<Integer, Integer>(r.user(), r.product()), r.rating());

           }

         }

     ));

     JavaRDD<Tuple2<Double, Double>> ratesAndPreds = 

       JavaPairRDD.fromJavaRDD(validation.map(

         new Function<Rating, Tuple2<Tuple2<Integer, Integer>, Double>>() {

           public Tuple2<Tuple2<Integer, Integer>, Double> call(Rating r){

             return new Tuple2<Tuple2<Integer, Integer>, Double>(

               new Tuple2<Integer, Integer>(r.user(), r.product()), r.rating());

           }

         }

     )).join(predictions).values();

     double MSE = JavaDoubleRDD.fromRDD(ratesAndPreds.map(

       new Function<Tuple2<Double, Double>, Object>() {

         public Object call(Tuple2<Double, Double> pair) {

           Double err = pair._1() - pair._2();

           return err * err;

         }

       }

     ).rdd()).mean();

     System.out.println("Mean Squared Error = " + MSE);

     answer = MSE;

     return answer;

 }

    private static List<Rating> getRecommendations(final int userId, MatrixFactorizationModel model, JavaRDD<Tuple2<Integer, Rating>> ratings, Map<Integer, String> products) {
        List<Rating> recommendations;

        //###########   Implement below part    #################//

        // your logic for generating movie recommendation for particular user
        // Use methods provided by spark to generate recommendation for particular user


        //Getting the users ratings
       
        JavaRDD<Rating> userRatings = ratings.filter(
                new Function<Tuple2<Integer, Rating>, Boolean>() {
                    public Boolean call(Tuple2<Integer, Rating> tuple) throws Exception {
                        return tuple._2().user() == userId;
                    }
                }
        ).map(
                new Function<Tuple2<Integer, Rating>, Rating>() {
                    public Rating call(Tuple2<Integer, Rating> tuple) throws Exception {
                        return tuple._2();
                    }
                }
        );

        //Getting the product ID's of the products that user rated
        JavaRDD<Tuple2<Object, Object>> userProducts = userRatings.map(
                new Function<Rating, Tuple2<Object, Object>>() {
                    public Tuple2<Object, Object> call(Rating r) {
                        return new Tuple2<Object, Object>(r.user(), r.product());
                    }
                }
        );

        List<Integer> productSet = new ArrayList<Integer>();
        productSet.addAll(products.keySet());

        Iterator<Tuple2<Object, Object>> productIterator = userProducts.toLocalIterator();

        //Removing the user watched (rated) set from the all product set
        while(productIterator.hasNext()) {
            Integer movieId = (Integer)productIterator.next()._2();
            if(productSet.contains(movieId)){
                productSet.remove(movieId);
            }
        }

        JavaRDD<Integer> candidates = sc.parallelize(productSet);

        JavaRDD<Tuple2<Integer, Integer>> userCandidates = candidates.map(
                new Function<Integer, Tuple2<Integer, Integer>>() {
                    public Tuple2<Integer, Integer> call(Integer integer) throws Exception {
                        return new Tuple2<Integer, Integer>(userId, integer);
                    }
                }
        );

        //Predict recommendations for the given user
        recommendations = model.predict(JavaPairRDD.fromJavaRDD(userCandidates)).collect();

        //Sorting the recommended products and sort them according to the rating
        Collections.sort(recommendations, new Comparator<Rating>() {
            public int compare(Rating r1, Rating r2) {
                return r1.rating() < r2.rating() ? -1 : r1.rating() > r2.rating() ? 1 : 0;
            }
        });

        //get top 5 from the recommended products.
        recommendations = recommendations.subList(0, 5);

        return recommendations;
    }


	}

