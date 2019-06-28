# README

This Java software package contains the backend classes used in the R package [largeRCRF](https://github.com/jatherrien/largeRCRF). Most users won't directly use this project, but it can be directly run by configuring a yaml settings file specifying all of the attributes about the random forest and dataset that you can. You're also free to integrate it into your own projects (as long as you follow the terms of the GPL-3 license), or to extend it. More documentation will be added later on how to extend it, but for now if you want an idea I suggest you take a look at the `MeanResponseCombiner` and `WeightedVarianceSplitFinder` classes, which is a small example of a regression random forest implementation. 


If you've made an extension or modification to the package and would like to integrate it into the R package component, build the project in Maven with `mvn clean package`, extract the contents of `largeRCRF-1.0-SNAPSHOT.jar` now found in the `target/` directory into the `inst/java/` directory for the R package (delete all the files previously there). Delete the `META-INF/` directory that was also extracted as that's meta information for the jar file and isn't relevant. Then just build the R package, possibly with your modifications in the R code, with `R> devtools::build()`.

If you have any questions on how to run this project, how to extend it, how to integrate it with R, or anything else related to this project, please feel free to either [email me](mailto:joelt@sfu.ca) or create an Issue. 

## System Requirements

You need:

* A Java runtime version 1.8 or greater
* Maven to build the project

## Troubleshooting (Running directly)

### I get an `OutOfMemoryException` error but I have plenty of RAM

By default the Java virtual machine only uses a quarter of the available system memory. When launching the jar file you can manually specify the memory available like below: 
```
java -jar -Xmx15G -Xms15G largeRCRF-1.0-SNAPSHOT.jar settings.yaml
```

with `15G` replaced with a little less than your available system memory.

### I get an `OutOfMemoryException` error and I'm short on RAM

Try reducing the number of trees being trained simultaneously by reducing the number of threads in the settings file.

### Training stalls immediately at 0 trees and the CPU is idle

This issue has been observed before on one particular system (and only on that system) but it's not clear what causes it. If you encounter this, please open an Issue and describe what operating system you're running on, what cloud system (if relevant) you're running on, and the entire output of `java --version`. 

From my observation this issues occurs randomly but only at 0 trees; so as an imperfect workaround you can cancel the training and try again. Another imperfect workaround is to set the number of threads to 1; this causes the code to not use Java's parallel code capabilities which will bypass the problem (at the cost of slower training).


