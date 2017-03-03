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