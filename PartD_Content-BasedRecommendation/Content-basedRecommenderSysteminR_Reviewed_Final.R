## Content-based Recommender system###########
########################Step 0 Install Packages ###############
## install package : recommenderlab
install.packages("recommenderlab", repos="http://cran.us.r-project.org")
install.packages("Matrix", repos="http://cran.us.r-project.org")
install.packages("registry", repos="http://cran.us.r-project.org")
install.packages("arules", repos="http://cran.us.r-project.org")
install.packages("lsa", repos="http://cran.us.r-project.org")
library("lsa")
library("recommenderlab")

########################Step 1 Data Preprocessing ###############
## Download data and get rating matrix (the utility matrix). Renamed 4 duplicated users: ID179, 705,831,913 to 17902,70502,83102,91302
utilitymatrix <- read.csv(file="~/SJSU_ClASSES/CMPE239_DataMining/Projects/Movie_night.csv", sep=",",header=TRUE)
ratingmatrix_noid <- as.vector(t(utilitymatrix[1:47,6:33]))
rownames <- utilitymatrix[1:47,2]
colnames <- c("Star Wars","Spotlight","Rocky","Mad Max: Fury Road","Kung Fu Panda 3", "Despicable Me","Big Hero 6","The hungover","Inside Out","The big short","Almost famous","The hunger games","Pulp Fiction","Inglourious Basterds","The usual suspects","Mamma mia","Frozen","Legally blonde","Silver Linings","Twilight","Bridget Jones diary","Gone girl","The Matrix","Alien","2001: A space Odyssey","The godfather","Pretty Woman","Pretty Woman.1")
ratingmatrix <- matrix(as.numeric(ratingmatrix_noid),nrow=47, ncol=28, byrow=TRUE,dimnames=list(rownames,colnames))
ratingmatrix

## coerce to realRatingMatrix
realratingmatrix <- as(ratingmatrix,"realRatingMatrix")
rowCounts(realratingmatrix) 
 
## The ID75 has 0 ratings, and ID254 has 2 ratings. Choose the data to contain users have 5 or more ratings, so the two IDs will not consider in.(row 8 and 25) 


realratingmatrix_valid <- realratingmatrix[-c(8,25),]
rowCounts(realratingmatrix_valid)
 
###### Step 2.  Create the Item Profile matrix. (Change the duplicated "Star Wars" to "Star Wars II")
Itemmatrix <- read.csv(file="~/SJSU_ClASSES/CMPE239_DataMining/Projects/Movie_night_genre.csv", sep=",",header=TRUE)
 
Itemmatrix_noid <- as.vector(t(Itemmatrix[1:28,2:8]))
Itemmatrix_noid[Itemmatrix_noid =="x"]<-1
 
irownames <- Itemmatrix[1:28,1]
icolnames <- c("Comedy","Action","Sci.Fi","Independent","Romantic","Animation","Drama")
Itemprofilematrix <- matrix(as.numeric(Itemmatrix_noid),nrow=28, ncol=7, byrow=TRUE,dimnames=list(irownames,icolnames))
 
Itemprofilematrix[is.na(Itemprofilematrix)] <-0
Itemprofilematrix 

######################## Step 3 Create User Profiles #############################
##### Create and calculating User Profile ##### using the new data(with no duplication) here.
movie <- data.frame(name <- c("Star Wars","Spotlight","Rocky","Mad Max: Fury Road","Kung Fu Panda 3", "Despicable Me","Big Hero 6","The hungover","Inside Out","The big short","Almost famous","The hunger games","Pulp Fiction","Inglourious Basterds","The usual suspects","Mamma mia","Frozen","Legally blonde","Silver Linings","Twilight","Bridget Jones diary","Gone girl","The Matrix","Alien","2001: A space Odyssey","The godfather","Pretty Woman","Pretty Woman.1"), label=1:28)
 
ratingmatrix_valid <- as(realratingmatrix_valid,"matrix") 
ratingmatrix_valid[is.na(ratingmatrix_valid)] <-0
matchingItemmatrix<-Itemprofilematrix[match(movie[,1],row.names(Itemprofilematrix)),,drop=FALSE]
matchingItemmatrix[is.na(matchingItemmatrix)] <-0
userprofile <- ratingmatrix_valid %*% matchingItemmatrix
userprofile[(userprofile >0)] <- 1
userprofile

##### Step 4 calculatig User Profile and Item profile Similarity ######
 
Similaritymatrix <- matrix(nrow=nrow(userprofile),ncol=nrow(Itemprofilematrix))
for(i in 1:nrow(userprofile)){
for(j in 1:nrow(Itemprofilematrix)){
Similaritymatrix[i,j] <- cosine(userprofile[i,],Itemprofilematrix[j,])
}
}
colnames(Similaritymatrix) <- irownames
Similaritymatrix

########################Step 5 Choose top n recommendations ######
 
## interception of movies with genre and movies in utility matrix
movieintercept <- function(x,y) {y[match(x,y,nomatch=0)]}
intercept <- movieintercept(movie[,1],row.names(Itemprofilematrix))
new_in_moviegenre <- row.names(Itemprofilematrix)[!row.names(Itemprofilematrix) %in% intercept]

Contentbased_rec <- function(userid,n)
{

## extract user rated and non-rated movies

id <- match(userid,row.names(ratingmatrix))
ratingmatrix[is.na(ratingmatrix)] <-0
userrating <- t(as.matrix(ratingmatrix[id,]))

userrated <- list()

usernotrated <- list()

for (i in 1:ncol(userrating)){

if (userrating[,i]==0){

usernotrated <- c(usernotrated,colnames(ratingmatrix)[i])

}

else

{

userrated <-c(userrated,colnames(ratingmatrix)[i])

}

}

usernotrated <- c(usernotrated,new_in_moviegenre)

userrated <-unlist(userrated)

usernotrated <- unlist(usernotrated)
 

## scores for non rated movies

notrated_score <- list()

names <- list()

for(j in 1:length(usernotrated)){

pred_score <- Similaritymatrix[id,which(rownames(t(Similaritymatrix))==usernotrated[j])]# rownames(t(Similaritymatrix)) = rownames(itemprofilematrix)

notrated_score <- c(notrated_score,pred_score)

names <- c(names,rownames(t(Similaritymatrix))[which(rownames(t(Similaritymatrix))==usernotrated[j])])

}

pred_score_for_user <- as.data.frame(notrated_score)

names(pred_score_for_user) <- names
   
   ## Sort the scores and return top N list
   sorted_score <- sort(pred_score_for_user,TRUE)
   top_score <- sorted_score[,1:n]
   return(top_score)
}
### Recommmdation test, using: user ID and top n movies you want.
Contentbased_rec(151,5)


####################### Evaluation #################################################
## Step 1 split data to training/testing( only the Intersection matrix maybe, otherwise cold start problem) ####
## use the 0.7/0.3 split and training/testing data
evaluator <- evaluationScheme(realratingmatrix_valid, method="split", train=0.7, given= 5)
trainingdata <- getData(evaluator, "train")
trainingdatamatrix<- as(trainingdata, "matrix")

testdata_known <- getData(evaluator, "known")
testdata_known_matrix<- as(testdata_known, "matrix")
testdata_unknown <- getData(evaluator, "unknown")
testdata_unknown_matrix<- as(testdata_unknown, "matrix")
 
## Step 2 use the training data to build the content-based recommendation. 

##### Create and calculating User Profile ; user profile include the trainingdatamatrix & testata_known_matrix #####
trainingdatamatrix_plus <- rbind(trainingdatamatrix,testdata_known_matrix)
f_userprofile <- function(x)
{
matchingItemmatrix<-Itemprofilematrix[match(movie[,1],row.names(Itemprofilematrix)),,drop=FALSE]
x[is.na(x)] <-0
matchingItemmatrix[is.na(matchingItemmatrix)] <-0
userprofile <- x %*% matchingItemmatrix
userprofile[(userprofile >0)] <- 1
return(userprofile) 
}
training_userprofile_plus <- f_userprofile(trainingdatamatrix_plus)
##### calculatig User Profile and Item profile Similarity ######
f_similarity <- function(a)
{
Similaritymatrix <- matrix(nrow=nrow(a),ncol=nrow(Itemprofilematrix))
for(i in 1:nrow(a)){
for(j in 1:nrow(Itemprofilematrix)){
Similaritymatrix[i,j] <- cosine(a[i,],Itemprofilematrix[j,])
}
}
colnames(Similaritymatrix) <- irownames
return(Similaritymatrix)
}
 
trained_similarity <- f_similarity(training_userprofile_plus)
 
## use the test data, find the items for unknown
 
Contentbased_rec_fortest <- function(userid,n)
{

## extract user rated and non-rated movies

## here we should use "known" test data to do prediction. id matching modified from matching testdata_known_matrix to trainingdatamatrix_plus

testdata_known_matrix[is.na(testdata_known_matrix)] <-0
trainingdatamatrix_plus[is.na(trainingdatamatrix_plus)] <-0

id <- match(userid,row.names(trainingdatamatrix_plus))

userrating <- t(as.matrix(trainingdatamatrix_plus[id,]))

userrated <- list()

usernotrated <- list()

for (i in 1:ncol(userrating)){

if (userrating[,i]==0){

usernotrated <- c(usernotrated,colnames(trainingdatamatrix_plus)[i])

}

else

{

userrated <-c(userrated,colnames(trainingdatamatrix_plus)[i])

}

}

## for the test set, we should not add new_in_moviegenre in.

##usernotrated <- c(usernotrated,new_in_moviegenre)

usernotrated <- c(usernotrated)

userrated <-unlist(userrated)

usernotrated <- unlist(usernotrated)
 

## scores for non rated movies

## The similarity matrix should use the one "trained" above. change it to be the new one,but no code change.

notrated_score <- list()

names <- list()

for(j in 1:length(usernotrated)){

pred_score <- trained_similarity[id,which(rownames(t(trained_similarity))==usernotrated[j])]# rownames(t(trained_similarity)) = rownames(itemprofilematrix)

notrated_score <- c(notrated_score,pred_score)

names <- c(names,rownames(t(trained_similarity))[which(rownames(t(trained_similarity))==usernotrated[j])])

}

pred_score_for_user <- as.data.frame(notrated_score)

names(pred_score_for_user) <- names
   
   ## Sort the scores and return top N list
   sorted_score <- sort(pred_score_for_user,TRUE)
   top_score <- sorted_score[,0:n]
   return(top_score)
}
testdata_unknown_matrix
Contentbased_rec_fortest(224,5)
 
### Recommmdation, using: unknown test data,  top-k recommendation.
 
testdata_unknown_matrix[is.na(testdata_unknown_matrix)] <-0
confusion_pred <- matrix(NA,nrow=nrow(testdata_unknown_matrix), ncol=ncol(testdata_unknown_matrix),byrow=TRUE,dimnames=list(rownames(testdata_unknown_matrix),colnames(testdata_unknown_matrix)))

recommend <- function(k)
{
for (i in 1:nrow(testdata_unknown_matrix))
{
rowscore <- Contentbased_rec_fortest(rownames(testdata_unknown_matrix)[i],k) ## predict/recommend @k
for (j in c(match(colnames(rowscore),colnames(confusion_pred))))
{
confusion_pred[i,j] <- 1
}
}
return(confusion_pred)
}

### Step 3 Calculate Precison@k#############
recommend_result<- recommend(5)

##### confusion matrix. change the confusion matrix to be: original test_unknown_data vs. test_unknown_data with top 5 recommendation 
 
recommend_result[is.na(recommend_result)] = -1
testdata_unknown_matrix[testdata_unknown_matrix>0] <-1
testdata_unknown_matrix[is.na(testdata_unknown_matrix)] <-0
testdata_unknown_matrix
confusionmatrix <- recommend_result - testdata_unknown_matrix
 
## precision@ 5
counttable <- table(confusionmatrix)
TP <- counttable[names(counttable)==0]
## precision_contentbased <- TP/(nrow(testdata_unknown_matrix)*(ncol(testdata_unknown_matrix)-5))
precision_contentbased <- TP/(nrow(testdata_unknown_matrix)*5)
precision_contentbased     ###



