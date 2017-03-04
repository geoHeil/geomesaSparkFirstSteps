// Copyright (C) 2017 Georg Heiler

package myOrg

import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.sql.SQLContext

object ExampleSQL extends App {

  val conf: SparkConf = new SparkConf()
    .setAppName("geomesaSparkStarter")
    .setMaster("local[*]")
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
  //    .set("spark.kryo.registrator", classOf[GeoMesaSparkKryoRegistrator].getName)
  //    .set("spark.sql.crossJoin.enabled", "true")

  val sp: SparkContext = new SparkContext(conf)
  val spark: SQLContext = new SQLContext(sp) // use sql context as long as hive metastore stuff or complex queries are not required (for 1.6)
  //  val spark: HiveContext = new HiveContext(sp)
  //  val spark: SparkSession = SparkSession
  //    .builder()
  //    .config(conf)
  //    .enableHiveSupport()
  //    .getOrCreate()

  //    import spark.implicits._

  val dsParams = Map(
    "instanceId" -> "instance",
    "zookeepers" -> "zoo1,zoo2,zoo3",
    "user" -> "user",
    "password" -> "*****",
    "auths" -> "USER,ADMIN",
    "tableName" -> "geomesa_catalog"
  )

  // Create DataFrame using the "geomesa" format
  val dataFrame = spark.read
    .format("geomesa")
    .options(dsParams)
    .option("geomesa.feature", "chicago")
    .load()
  dataFrame.show
  dataFrame.registerTempTable("chicago")
  //  dataFrame.createOrReplaceTempView("chicago")

  // Query against the "chicago" schema
  val sqlQuery = "select * from chicago where st_contains(st_makeBBOX(0.0, 0.0, 90.0, 90.0), geom)"
  val resultDataFrame = spark.sql(sqlQuery)

  resultDataFrame.show

  // same thing using dataframe API - do not yet know how to use it
  //  dataFrame.filter(st_contains(st_makeBBOX(0.0, 0.0, 90.0, 90.0), 'geom)).show

  sp.stop

}
