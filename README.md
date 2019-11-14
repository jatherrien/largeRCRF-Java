# README

This repository contains the largeRCRF Java library, containing all of the logic used for training the random forests. This provides the Jar file used in the R package [largeRCRF](https://github.com/jatherrien/largeRCRF).

Most users interested in training random competing risks forests should use the [R package component](https://github.com/jatherrien/largeRCRF); the content in this repository is only useful for advanced users. 

## License

You're free to use / modify / redistribute the project, as long as you follow the terms of the GPL-3 license. 

## Extending

Documentation on how to extend the library to add support for other types of random forests will eventually be added, but for now if you're interested in that I suggest you take a look at the `MeanResponseCombiner` and `WeightedVarianceSplitFinder` classes to see how some basic regression random forests were introduced. 

If you've made a modification to the package and would like to integrate it into the R package component, build the project in Maven with `mvn clean package`, then just copy `target/largeRCRF-library-1.0-SNAPSHOT.jar` into the `inst/java/` directory for the R package, replacing the previous Jar file there. Then build the R package, possibly with your modifications to the code there, with `R> devtools::build()`.

Please don't take the current lack of documentation as a sign that I oppose others extending or modifying the project; if you have any questions on running, extending, integrating with R, or anything else related to this project, please don't hesitate to either [email me](mailto:joelt@sfu.ca) or create an Issue. Most likely my answers to your questions will end up forming the basis for any documentation written. 

## System Requirements

You need:

* A Java runtime version 1.8 or greater
* Maven to build the project

