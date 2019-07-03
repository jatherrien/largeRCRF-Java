# README

This repository contains two Java projects; 

* The first is the largeRCRF library (`library/`) containing all of the logic used for training the random forests. This part provides the Jar file used in the R package [largeRCRF](https://github.com/jatherrien/largeRCRF).
* The second is a small executable (`executable/`) Java project that uses the library and can be run directly outside of R. It's still in its early stages and isn't polished, nor is it yet well documented; but you can take a look if you want.

Most users interested in training random competing risks forests should use the [R package component](https://github.com/jatherrien/largeRCRF); the content in this repository will only be useful to advanced users. 

## License

You're free to use / modify / redistribute either of the two projects above, as long as you follow the terms of the GPL-3 license. 

## Extending

Documentation on how to extend the library to add support for other types of random forests will eventually be added, but for now if you're interested in that I suggest you take a look at the `MeanResponseCombiner` and `WeightedVarianceSplitFinder` classes to see how some basic regression random forests were introduced. 

If you've made a modification to the package and would like to integrate it into the R package component, build the project in Maven with `mvn clean package` (in the same directory as this `README` file), then just copy `library/target/largeRCRF-library-1.0-SNAPSHOT.jar` into the `inst/java/` directory for the R package, replacing the previous Jar file there. Then build the R package, possibly with your modifications to the code there, with `R> devtools::build()`.

Please don't take the current lack of documentation as a sign that I oppose others extending or modifying the project; if you have any questions on running, extending, integrating with R, or anything else related to this project, please don't hesitate to either [email me](mailto:joelt@sfu.ca) or create an Issue. Most likely my answers to your questions will end up forming the basis for any documentation written. 

## System Requirements

You need:

* A Java runtime version 1.8 or greater
* Maven to build the project

## Troubleshooting (Running `executable`)

Some of these Troubleshooting items can also apply if you are integrating the library classes into your own Java project.

### I get an `OutOfMemoryException` error but I have plenty of RAM

By default the Java virtual machine only uses a quarter of the available system memory. When launching the Jar file you can manually specify the memory available like below: 
```
java -jar -Xmx15G -Xms15G largeRCRF-executable-1.0-SNAPSHOT.jar settings.yaml
```

with `15G` replaced with a little less than your available system memory.

### I get an `OutOfMemoryException` error and I'm short on RAM

Try reducing the number of trees being trained simultaneously by reducing the number of threads in the settings file.

### Training stalls immediately at 0 trees and the CPU is idle

This issue has been observed before on one particular system (and only on that system) but it's not clear what causes it. If you encounter this, please open an Issue and describe what operating system you're running on, what cloud system (if relevant) you're running on, and the entire output of `java --version`. 

From my observation this issues occurs randomly but only at 0 trees; so as an imperfect workaround you can cancel the training and try again. Another imperfect workaround is to set the number of threads to 1; this causes the code to not use Java's parallel code capabilities which will bypass the problem (at the cost of slower training).

