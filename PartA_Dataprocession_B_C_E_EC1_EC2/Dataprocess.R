# Part A
rate <- read.csv("movie_rating.csv",header=TRUE,sep = ",")

# Rating dataset - Col1: userID; Col2: itemID; Col3: Rating (without header)
user <- data.frame(rate[,6:42])
names(user)<-sprintf("M%d",1:37)
user <- data.frame(ID = rate$ID, user)
user$ID[duplicated(user$ID)] <- sub("$", "02", user$ID[duplicated(user$ID)]) # no duplication
dataUser <- reshape(user, direction = "long", varying = 2:38, sep = "")
dataUser <- dataUser[complete.cases(dataUser),]
User <- data.frame(userID = as.numeric(dataUser$ID), itemID = dataUser$time, value = dataUser$M)
write.table(User, file = "UserdatasetN.csv", col.names = FALSE, row.names = FALSE,sep=",")

# Movie Label - Col1: item ID; Col2: Movie name
mname <- read.csv("movie_rating.csv",header=FALSE, nrows=1)
mname <- mname[,6:42]
mname <- reshape(mname, direction = "long",varying = 1:37, sep = "")
label <- data.frame(itemID = c(1:37), M = tolower(mname$V))
write.csv(label, file = "mLabel.csv", row.names = FALSE) # with column name (for part EC2)
write.table(label, file = "movies.csv", col.names = FALSE, row.names = FALSE,sep=",") # without column name (for part E)

# Clean the movie genre table to obtain a binary matrix, which will used in EC2.
movie <- read.csv("MovieGenre.csv",header=TRUE,sep = ",")
movie <- movie[!duplicated(movie[,1]), ] # delete the duplicate movie
movie <- as.matrix(movie)
movie[movie == "x"] <- 1
movie[movie == ""] <- 0
write.table(movie, file = "Movie.csv", col.names = TRUE, row.names = FALSE,sep=",")

# Dataset for Part E which is added a new column with integer 1:100
User <- read.csv("UserdatasetN.csv",header=FALSE,sep = ",")
set.seed(123)
timestamp <- sample(1:100, 670,replace=TRUE) 
User.new <- cbind(User,timestamp)
write.table(User.new, file = "ratings.csv", col.names = FALSE, row.names = FALSE,sep=",")

# Part EC2
# reshape the movie/genre table
movie <- read.csv("Movie.csv",header=TRUE,sep = ",")
mtype <- data.frame(movie[,2:8])
colnames(mtype)<-sprintf("T%d",1:7)
movie <- data.frame(M = movie[,1], mtype)
dataType <- reshape(movie, direction = "long", varying = 2:8, sep = "")
dataType <- data.frame(M = tolower(dataType$M), Genre = dataType$time, T=dataType$T)

# use Label file to substitute movies name with itemID. Make genres look like new users. 
Label <- read.csv("mLabel.csv",header=TRUE,sep = ",")
mGenre <- merge(Label, dataType, by = "M")
mGenre$Genre <- sub("^", "999", mGenre$Genre) # rename genre so that they won't be confused with userID
UmGenre <- data.frame(userID = mGenre$Genre, itemID = mGenre$itemID, value = mGenre$T)
write.table(UmGenre, file = "Userdataset2.csv", col.names = FALSE, row.names = FALSE,sep=",")

# combine previous dataset with genre/item data to enrich the item vectors.
datafile1 <- read.csv("UserdatasetN.csv", header=F, sep=",")
datafile2 <- read.csv("Userdataset2.csv", header=F, sep=",")
datafile <- rbind(datafile1,datafile2)
write.table(datafile, file = "HybriddatasetN.csv", col.names = FALSE, row.names = FALSE,sep=",")
