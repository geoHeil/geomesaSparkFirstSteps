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

**index join**

What I want to achieve is:
- given a list of  Points(x,y) and a list of MultiPolygons
- determine which is contained in which MultiPolygons
- a single point can be contained in 0, 1, or multiple Polygons
- i.e. I want to compare n points with m polygons where `#n >> #m`

Some thoughts

- we might need an R tree to do efficient multi polygon lookups
    - see chat here https://gitter.im/locationtech/geomesa with comments of @jnh5y 
    > I think I'd just write the obvious filters against a list of bounding boxes
      if I recall, the GeoMesa In-Memory module creating a simple index for points
      since it doesn't support bounding boxes / non-point geometries, you'd need to grab JTS's R-Tree implementation and roll something manually
    > they aren't running a query for each point and polygon
      they are iterating over the list of polygons and issuing queries for each one of those against the other data source
      since for each point, you want a list of polys containing it, it'd iterate over points and ask for bounding boxes which contain it
     
    - or iteratively check for each point if `WITHIN` or `WITHIN` evaluates to true
- java R tree implementations, but do not seem to handle multi polygons
    - https://github.com/meetup/archery
    - https://github.com/davidmoten/rtree
    - http://locationtech.github.io/jts/javadoc/org/locationtech/jts/index/strtree/STRtree.html is possibly the most useful implementation so far as query only but optimized for that
    
- it would also be possible to start a postGIS instance and use http://revenant.ca/www/postgis/workshop/joins.html if I reduce the data solely to geo spatial information and process the rest of the data in hive / spark and put only the geo-spatial data into the GIS system. Maybe scalable enough but not a single platform.
- this is using `intersects` queries equally implemented in geomesa memory https://github.com/locationtech/geomesa/blob/master/geomesa-memory/geomesa-cqengine/src/test/scala/org/locationtech/geomesa/memory/cqengine/utils/SampleFeatures.scala#L184-L198 And as far as I understand the thing still would need to check this for any of the m multyPolygons. Is this correct?
- geotools (java) has a join example as well http://docs.geotools.org/latest/javadocs/org/geotools/data/Join.html with an example here http://revenant.ca/www/postgis/workshop/joins.html
- http://getindata.com/blog/post/geospatial-analytics-on-hadoop/ is some great overview
- other geo spatial spark solutionsgeospgeosp
    - http://geospark.datasyslab.org/ spatial join example directly on their site, but only seems to use point with rectangle and not multypolygon, based on R tree as well
    - https://github.com/harsha2010/magellan