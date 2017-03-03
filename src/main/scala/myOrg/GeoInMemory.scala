// Copyright (C) 2017 Georg Heiler

package myOrg

import java.sql.Date

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.Row
// TODO fix imports below none seems to be imported
import org.geotools.filter.text.ecql.ECQL
import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.locationtech.geomesa.memory.cqengine.GeoCQEngine

object GeoInMemory extends App {

  val conf: SparkConf = new SparkConf()
    .setAppName("geomesaSparkInMemory")
    .setMaster("local[*]")
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
  //    .set("spark.sql.crossJoin.enabled", "true")

  val sp: SparkContext = new SparkContext(conf)
  val spark: HiveContext = new HiveContext(sp)
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
    "Who:String:index=full",
    "id:Integer",
    "date:Date",
    "*Where:Point:srid=4326",
    "Why:String"
  ).mkString(",")

  // instead for now manually some data from chicago
  val df = Seq(
    (1, "20160101T000000.000Z", -76.5, 38.5, "foo"),
    (2, "20160102T000000.000Z", -77.0, 38.0, "bar"),
    (3, "20160103T000000.000Z", -78.0, 39.0, "foo")
  ).toDF("id", "date", "coordX", "coordY", "someProperty").as[SimpleChicago]

  df.show
  // assuming medum sized data, lets collect it locally
  // only for local data we can use https://github.com/locationtech/geomesa/tree/master/geomesa-memory with great indexing

  //val localData = df.collect // to somple, we need to map to collection of feature type
  df.map(_.coordX)
  // this is simpler as dataset holds type information, TODO mapt to fitting simple feature
  val localData = df.map { case Row(d, date, coordX, coordY, someProperty) => Map("period" -> period, "totalAmountLabel" -> sumTotalAmount) }.collect


  // create index

  val spec = "Who:String:cq-index=default,*Where:Point:srid=4326"
  val sft = SimpleFeatureTypes.createType("test", spec)

  def buildFeature(sft: SimpleFeatureType, fid: Int): SimpleFeature = NoneFoobar

  val feats = (0 until 1000).map(buildFeature(sft, _))
  val newfeat = buildFeature(sft, 1001)

  // create a new cache
  val cq = new GeoCQEngine(sft)

  // add a collection of features
  cq.addAll(feats)

  // clear the cache
  cq.clear()

  // get a FeatureReader with all features that match a filter
  val f = ECQL.toFilter("Who = 'foo' AND BBOX(Where, 0, 0, 180, 90)")
  val reader = cq.getReaderForFilter(f)

  // execute query


  // put the result back into spark

  // send back to hive
}
