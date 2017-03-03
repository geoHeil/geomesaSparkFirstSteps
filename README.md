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