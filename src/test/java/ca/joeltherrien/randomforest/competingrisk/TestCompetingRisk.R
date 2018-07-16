# Comparing output between my two packages

require(survival)
require(randomForestSRC)

data(wihs, package = "randomForestSRC")

wihs$idu = as.logical(wihs$idu)
wihs$black = as.logical(wihs$black)

set.seed(100)

wihs = wihs[sample(1:nrow(wihs), replace = FALSE),]

# example row
#    time status ageatfda   idu black cd4nadir
#409  1.3      1       35 FALSE FALSE     0.81
newData = data.frame(ageatfda=35, idu=FALSE, black=FALSE, cd4nadir=0.81)

one.tree <- rfsrc(Surv(time, status) ~ idu + black, wihs, nsplit = 5, ntree = 1, splitrule="logrank", cause=1, mtry=2, seed=-5, membership=TRUE)
# next get membership
membership=one.tree$inbag[,1]

if(FALSE){
  bootstrappedData = wihs[c(),]
  for(i in 1:length(membership)){
    times = membership[i]
    
    if(times > 0){
      for(j in 1:times){
        bootstrappedData = rbind(bootstrappedData, wihs[i,])
      }
      
    }
  }
}
output.one.tree = predict(one.tree, newData)
output.one.tree$cif[,,1]
output.one.tree$chf[,c(11,66,103),1]

# Note that "... ~ ." means 'use all explanatory variables"
#output = predict(wihs.obj, newData)
#output$cif[,,1] # CIF for cause 1
#output$cif[,,2] # CIF for cause 2

many.trees <- rfsrc(Surv(time, status) ~ idu + black, wihs, nsplit = 5, ntree = 100, splitrule="logrank", cause=1, mtry=2, membership=TRUE)
output.many.trees = predict(many.trees, newData)
output.many.trees$cif[,41,1]
output.many.trees$cif[,41,2]

many.trees.all <- rfsrc(Surv(time, status) ~ ageatfda + cd4nadir + idu + black, wihs, nsplit = 5, ntree = 100, splitrule="logrank", cause=1, mtry=2, membership=TRUE)
output.many.trees.all = predict(many.trees.all, newData)
output.many.trees.all$cif[,103,1]
output.many.trees.all$cif[,103,2]





end.numbers = c()
end.times = c()
lgths = c()
trees = list()
for(i in 1:100){
  one.tree = rfsrc(Surv(time, status) ~ ageatfda + cd4nadir + idu + black, wihs, nsplit = 0, ntree = 1, splitrule="logrank", cause=1, mtry=4, membership=TRUE, statistics = TRUE)
  trees[[i]] = one.tree
  prediction = predict(one.tree, newData)
  lgth = length(prediction$cif[,,1])
  lgths = c(lgths, lgth)
  end.numbers = c(end.numbers, prediction$cif[,lgth,1])
  end.times = c(end.times, max(prediction$time.interest))
}

special.tree = trees[[100]]


prediction = predict(special.tree, newData)
prediction$cif[,,1]






membership = special.tree$inbag[,1]
bootstrappedData = wihs[c(),]
for(i in 1:length(membership)){
  times = membership[i]
  
  if(times > 0){
    for(j in 1:times){
      bootstrappedData = rbind(bootstrappedData, wihs[i,])
    }
    
  }
}
write.csv(bootstrappedData, "RandomSurvivalForests/src/test/resources/wihs.bootstrapped2.csv", row.names=FALSE)
prediction$cif[,,1]

