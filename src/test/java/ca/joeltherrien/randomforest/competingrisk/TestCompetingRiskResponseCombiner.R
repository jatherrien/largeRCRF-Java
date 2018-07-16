testData = data.frame(delta=c(1,1,1,2,2,0,0), u=c(1,1,2,1.5,2,1.5,2.5))
testData$survDelta = ifelse(testData$delta==0, 0, 1) # for KM curves on any events

require(survival)

kmCurve = survfit(Surv(u, survDelta, type="right") ~ 1, data=testData)
kmCurve$surv

curve = survfit(Surv(u, event=delta, type="mstate") ~ 1, data=testData)
curve$cumhaz[3,1:2,]

print(t(curve$pstate[,1:2]))
