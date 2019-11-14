testData1 = data.frame(delta=c(1,1,1,1,0,0,0), u=c(1,1,2,1.5,2,1.5,2.5), group=TRUE)
testData2 = data.frame(delta=c(1,1,1,1,0,0,0), u=c(2,2,4,3,4,3,5), group=FALSE) # just doubled everything

testData = rbind(testData1, testData2)
require(survival)

results = survdiff(Surv(u, delta, type="right") ~ group, data=testData)

mantelTest = function(times, observed, group0, group1=!group0){
  U0 = times[group0]
  observed0 = observed[group0]
  U1 = times[group1]
  observed1 = observed[group1]
  
  Vs = sort(unique(c(U0[observed0], U1[observed1])))
  
  atRisk = function(v, u){
    u = subset(u, u >= v)
    return(length(u))
  }
  
  Os = c()
  Es = c()
  varOs = c()
  
  # we're going to treat group 1 as treatment
  for(v in Vs){
    placeboAtRisk = atRisk(v, U0)
    treatmentAtRisk = atRisk(v, U1)
    totalAtRisk = placeboAtRisk + treatmentAtRisk
    
    numTreatmentFailures = length(subset(U1, observed1 & U1 == v))
    numPlaceboFailures = length(subset(U0, observed0 & U0 == v))
    totalFailures = numTreatmentFailures + numPlaceboFailures
    
    Os = c(Os, numTreatmentFailures)
    Es = c(Es, (totalFailures)*treatmentAtRisk/totalAtRisk)
    
    varOfO = (totalAtRisk - treatmentAtRisk)/(totalAtRisk - 1) *
      treatmentAtRisk * (totalFailures / totalAtRisk) *
      (1 - totalFailures / totalAtRisk)
    
    if(totalAtRisk == 1){
      varOfO = 0
    }
    
    varOs = c(varOs, varOfO)
  }
  
  numerator = sum(Os - Es)
  variance = sum(varOs)
  
  Z = numerator/sqrt(variance)
  return(list(
    statistic = Z,
    pvalue = 2*pnorm(abs(Z), lower.tail=FALSE),
    numerator = numerator,
    variance = variance
  ))
}

myTest = mantelTest(testData$u, testData$delta == 1, group0=testData$group==1)
