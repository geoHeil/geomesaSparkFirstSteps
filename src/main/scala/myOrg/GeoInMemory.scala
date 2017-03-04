// Copyright (C) 2017 Georg Heiler

package myOrg

import java.sql.Date

import com.vividsolutions.jts.geom.Point
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.sql.{ Row, SQLContext, SaveMode }
//import org.geotools.factory.CommonFactoryFinder
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.locationtech.geomesa.spark.GeoMesaSparkKryoRegistrator
import org.locationtech.geomesa.utils.text.WKTUtils
//import org.opengis.filter.Filter

import scala.util.Random
// TODO fix imports below none seems to be imported
import org.geotools.filter.text.ecql.ECQL
import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes
import org.opengis.feature.simple.{ SimpleFeature, SimpleFeatureType }
import org.locationtech.geomesa.memory.cqengine.GeoCQEngine
import collection.JavaConversions._

object GeoInMemory extends App {

  val conf: SparkConf = new SparkConf()
    .setAppName("geomesaSparkInMemory")
    .setMaster("local[*]")
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .set("spark.kryo.registrator", classOf[GeoMesaSparkKryoRegistrator].getName)
  //    .set("spark.sql.crossJoin.enabled", "true")

  val sp: SparkContext = new SparkContext(conf)
  val spark: SQLContext = new SQLContext(sp) // use sql context as long as hive metastore stuff or complex queries are not required (for 1.6)
  //  val spark: HiveContext = new HiveContext(sp)
  // below code for 2.0
  //  val spark: SparkSession = SparkSession
  //    .builder()
  //    .config(conf)
  //    .enableHiveSupport()
  //    .getOrCreate()

  import spark.implicits._
  // TODO get data from hive, requires hive_metastore.xml be configured correctly and passed to spark
  //  val df = spark.sql(
  //    """‚
  //      |SELECT *
  //      |FROM hive_table
  //    """.stripMargin)

  // spark schema for DataSet
  case class SimpleChicago(id: Int, date: Date, coordX: Float, coordY: Float, someProperty: String)

  // geomesa index information, see readme for details how to configure index
  // this will not be used here, but rather in map function!!!!
  val spec = List(
    "id:Integer",
    "date:Date",
    "*location:Point:srid=4326",
    "someProperty:String"
  ).mkString(",")
  val sft = SimpleFeatureTypes.createType("firststeps", spec)

  // TODO try some indices
  //  val specIndexes = List(
  //    "id:Integer:cq-index=navigable",
  //    "date:Date:cq-index=navigable",
  //    "*location:Point:srid=4326",
  //    "someProperty:String"
  //  ).mkString(",")
  //  val sftWithIndexes = SimpleFeatureTypes.createType("firststeps2", specIndexes)

  val builder = new SimpleFeatureBuilder(sft)

  // TODO map values to correct format i.e. coordinates to point an the whole thing to a collection of simple feature type
  val x = Random.nextDouble
  val y = Random.nextDouble
  WKTUtils.read(s"POINT($x $y)").asInstanceOf[Point]

  // instead for now manually some data from chicago
  val df = Seq(
    (1, "2016-01-01", -76.5, 38.5, "foo"),
    (2, "2016-01-01", -77.0, 38.0, "bar"),
    (3, "2016-ß2-02", -78.0, 39.0, "foo")
  ).toDF("id", "date", "coordX", "coordY", "someProperty").as[SimpleChicago]

  df.show
  // assuming medum sized data, lets collect it locally
  // only for local data we can use https://github.com/locationtech/geomesa/tree/master/geomesa-memory with great indexing

  def buildFeature(f: SimpleChicago): SimpleFeature = {
    // TODO infer this mapping with less boilerplate code
    builder.set("date", f.date)
    builder.set("location", WKTUtils.read(s"POINT(${f.coordX} ${f.coordY})").asInstanceOf[Point])
    builder.set("someProperty", f.someProperty)
    builder.buildFeature(f.id.toString)
  }

  //val localData = df.collect // to somple, we need to map to collection of feature type
  val localData = df.map(buildFeature(_)).collect

  // create a new cache
  val cq = new GeoCQEngine(sft)

  // add a collection of features
  cq.addAll(localData.toSeq)

  // clear the cache
  cq.clear()

  // TODO setup some convenience functions for easier queries
  // get a FeatureReader with all features that match a filter
  //  implicit def stringToFilter(s: String): Filter = ECQL.toFilter(s)

  //  val ff = CommonFactoryFinder.getFilterFactory2

  val f = ECQL.toFilter("someProperty = 'foo' AND BBOX(Where, 0, 0, 180, 90)")
  val reader = cq.getReaderForFilter(f)

  // todo play with queries from https://github.com/locationtech/geomesa/blob/master/geomesa-memory/geomesa-cqengine/src/test/scala/org/locationtech/geomesa/memory/cqengine/utils/SampleFeatures.scala#L104-L259

  // put the result back into spark
  // TODO parallelize queried result

  // send back to hive, requires hive context!
  // TODO find a better way to store data in hive
  //  df.toDF.saveAsTable("persitentFilteredData") // this is deprecated and removed in spark 2.0
  //  df.toDF.registerTempTable("filteredData")
  //  spark.sql("create table persitentFilteredData as select * from filteredData");

  // TODO create table as ORC see: https://de.hortonworks.com/hadoop-tutorial/using-hive-with-orc-from-apache-spark/
  // just add `AS ORC` at the end

  sp.stop
}
