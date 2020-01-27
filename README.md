# standard-quality-metrics

In this repository, you can find quality metrics to check the standardization and quality of Geospatial Linked Data. You can integrate these metrics with Luzzu framework (or any other tool that you wish) to assess the quality of your data and conformance to the standards. 

## Pre-requisites
- [Luzzu framework](https://github.com/Luzzu/Framework)
- Java 1.8 with Maven


## Steps to Follow
- Copy Luzzu framework to your local folder
- Build Luzzu framework (More info could be found in framework's github page)
- Download the Geospatial Linked Data qualilty metrics
- Place the metrics in the `luzzu-communications/externals/metrics/` folder.
- For Luzzu, in order for these metrics to work, download the vocabularies as well from [here](http://s001.adaptcentre.ie/FrameworkMetrics/Vocabs/). These should be uncompressed, and the ttl files should be placed in the `luzzu-communications/externals/vocabs/` folder. The `dqm.zip` contain all semantic definitions for the quality metrics in the previous section.

## Executing Luzzu
- Go to project folder
- Execute start.sh and a web service will be started
