package stats.MovieRecommenderSystem;

import scala.Tuple2;

import org.apache.spark.api.java.*;
import org.apache.spark.rdd.RDD;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;

import java.util.*;
import org.apache.spark.api.java.function.*;
import org.apache.spark.mllib.evaluation.RegressionMetrics;
import org.apache.spark.mllib.evaluation.RankingMetrics;
import org.apache.spark.mllib.recommendation.ALS;
import org.apache.spark.mllib.recommendation.Rating;

// Read in the ratings data
public class MatrixFactorBasedEvaluator {


    static SparkConf conf;
    static JavaSparkContext sc;

    public static void main(String args[])
        {
    //Initializing Spark
         
    	conf = new SparkConf().setAppName("MovieRecommendation").setMaster("local");
        sc = new JavaSparkContext(conf);

            //Reading Data
    final JavaRDD<String> ratingData = sc.textFile("/Users/itsworthmore/SJSU_ClASSES/CMPE239_DataMining/Projects/ratings.csv");
    JavaRDD<String> productData = sc.textFile("/Users/itsworthmore/SJSU_ClASSES/CMPE239_DataMining/Projects/movies.csv");


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

                long numTraining = training.count();
    	        long numValidation = validation.count();
    	        long numTest = test.count();

    	    System.out.println("Training: " + numTraining + ", validation: " + numValidation + ", test: " + numTest);


    // Train an ALS model
    //final MatrixFactorizationModel model = ALS.train(JavaRDD.toRDD(ratings), 10, 10, 0.05);
     MatrixFactorizationModel model1 = ALS.train(JavaRDD.toRDD(training),10, 6,0.1);

    // Get top 5 recommendations for every user and scale ratings from 0 to 1
    JavaRDD<Tuple2<Object, Rating[]>> userRecs = model1.recommendProductsForUsers(5).toJavaRDD();
    JavaRDD<Tuple2<Object, Rating[]>> userRecsScaled = userRecs.map(
      new Function<Tuple2<Object, Rating[]>, Tuple2<Object, Rating[]>>() {
        public Tuple2<Object, Rating[]> call(Tuple2<Object, Rating[]> t) {
          Rating[] scaledRatings = new Rating[t._2().length];
          for (int i = 0; i < scaledRatings.length; i++) {
            double newRating = Math.max(Math.min(t._2()[i].rating(), 1.0), 0.0);
            scaledRatings[i] = new Rating(t._2()[i].user(), t._2()[i].product(), newRating);
          }
          return new Tuple2<Object, Rating[]>(t._1(), scaledRatings);
        }
      }
    );
    JavaPairRDD<Object, Rating[]> userRecommended = JavaPairRDD.fromJavaRDD(userRecsScaled);

    // Map ratings to 1 or 0, 1 indicating a movie that should be recommended
    JavaRDD<Rating> binarizedRatings = test.map(
      new Function<Rating, Rating>() {
        public Rating call(Rating r) {
          double binaryRating;
          if (r.rating() > 0.0) {
            binaryRating = 1.0;
          }
          else {
            binaryRating = 0.0;
          }
          return new Rating(r.user(), r.product(), binaryRating);
        }
      }
    );

    // Group ratings by common user
    JavaPairRDD<Object, Iterable<Rating>> userMovies = binarizedRatings.groupBy(
      new Function<Rating, Object>() {
        public Object call(Rating r) {
          return r.user();
        }
      }
    );

    // Get true relevant documents from all user ratings
    JavaPairRDD<Object, List<Integer>> userMoviesList = userMovies.mapValues(
      new Function<Iterable<Rating>, List<Integer>>() {
        public List<Integer> call(Iterable<Rating> docs) {
          List<Integer> products = new ArrayList<Integer>();
          for (Rating r : docs) {
            if (r.rating() > 0.0) {
              products.add(r.product());
            }
          }
          return products;
        }
      }
    );

    // Extract the product id from each recommendation
    JavaPairRDD<Object, List<Integer>> userRecommendedList = userRecommended.mapValues(
      new Function<Rating[], List<Integer>>() {
        public List<Integer> call(Rating[] docs) {
          List<Integer> products = new ArrayList<Integer>();
          for (Rating r : docs) {
            products.add(r.product());
          }
          return products;
        }
      }
    );
    JavaRDD<Tuple2<List<Integer>, List<Integer>>> relevantDocs = userMoviesList.join(userRecommendedList).values();

    // Instantiate the metrics object
    RankingMetrics metrics = RankingMetrics.of(relevantDocs);

    // Precision and NDCG at k
    Integer[] kVector = {1, 3, 5};
    for (Integer k : kVector) {
      System.out.format("Precision at %d = %f\n", k, metrics.precisionAt(k));
      //System.out.format("NDCG at %d = %f\n", k, metrics.ndcgAt(k));
    }

    // Mean average precision
    System.out.format("Mean average precision = %f\n", metrics.meanAveragePrecision());

    // Evaluate the model using numerical ratings and regression metrics
    JavaRDD<Tuple2<Object, Object>> userProducts = test.map(
      new Function<Rating, Tuple2<Object, Object>>() {
        public Tuple2<Object, Object> call(Rating r) {
          return new Tuple2<Object, Object>(r.user(), r.product());
        }
      }
    );
    JavaPairRDD<Tuple2<Integer, Integer>, Object> predictions = JavaPairRDD.fromJavaRDD(
      model1.predict(JavaRDD.toRDD(userProducts)).toJavaRDD().map(
        new Function<Rating, Tuple2<Tuple2<Integer, Integer>, Object>>() {
          public Tuple2<Tuple2<Integer, Integer>, Object> call(Rating r){
            return new Tuple2<Tuple2<Integer, Integer>, Object>(
              new Tuple2<Integer, Integer>(r.user(), r.product()), r.rating());
          }
        }
    ));
    JavaRDD<Tuple2<Object, Object>> ratesAndPreds =
      JavaPairRDD.fromJavaRDD(test.map(
        new Function<Rating, Tuple2<Tuple2<Integer, Integer>, Object>>() {
          public Tuple2<Tuple2<Integer, Integer>, Object> call(Rating r){
            return new Tuple2<Tuple2<Integer, Integer>, Object>(
              new Tuple2<Integer, Integer>(r.user(), r.product()), r.rating());
          }
        }
    )).join(predictions).values();

    // Create regression metrics object
    RegressionMetrics regressionMetrics = new RegressionMetrics(ratesAndPreds.rdd());

    // Root mean squared error
    System.out.format("RMSE = %f\n", regressionMetrics.rootMeanSquaredError());

  }
}
