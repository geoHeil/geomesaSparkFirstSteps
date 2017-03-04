# geomesa spark in memory
mini project to show geomesa (in memory), spark and hive might be integrated

use `sbt console`to interactively run queries

or `./sync.sh` to run assembly

or `sbt run` but make sure to set `$SBT_OPTS -Xmx8G -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Xss2M`
as spark will be launched inside sbt 


**dependencies**

```
git clone https://github.com/locationtech/geomesa.git
cd geomesa
mvn install -DskipTests=true
```

**note**

- currently this is configured for spark 2.1
- for HDP 2.5 install spark 2 preview
- make sure that spark-shell will not start spark 1.6
- to access hive place the metastore information into the resources folder?


**index configuration**

- good usage examples
    - https://github.com/locationtech/geomesa/blob/master/geomesa-memory/geomesa-cqengine/src/test/scala/org/locationtech/geomesa/memory/cqengine/utils/SampleFeatures.scala#L104-L259
- http://www.geomesa.org/documentation/user/accumulo/data_management.html#attribute-indices

**process overview**

1. get data from hive into spark
2. map the data to simpleFeature type
3. collect to local JVM
4. create index
5. query index
6. re-parallelize results into a spark data frame
7. write results back to hive