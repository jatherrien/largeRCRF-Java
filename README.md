# README

This Java software package contains the backend classes used in the R package [largeRCRF](https://github.com/jatherrien/largeRCRF). 

On its own it's not useful, but you're free to integrate it into your own projects (as long as you follow the terms of the GPL-3 license), or extend it. More documentation will be added later on how to extend it, but for now if you want an idea I suggest you take a look at the `MeanResponseCombiner` and `WeightedVarianceSplitFinder` classes, which is a small example of a regression random forest implementation. 

If you've made an extension or modification to the package and would like to integrate it into the R package component, build the project in Maven with `mvn clean package` and copy the `largeRCRF-1.0-SNAPSHOT.jar` file now found in the `target/` directory into the `inst/java/` directory for the R package (delete the previous jar file). Then just build the R package, possibly with your modifications in the R code, with `R> devtools::build()`.

If you have any questions on how to integrate this code with your own, how to integrate it with the R project, or anything else related to this project, please feel free to either [email me](mailto:joelt@sfu.ca) or create an Issue. 

A small project allowing this code to be called directly outside of R will be released soon.

## System Requirements

You need:

* A Java runtime version 1.8 or greater
* Maven to build the project



