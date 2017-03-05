name := "geomesaSparkStarter"
organization := "myOrg"

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-Xfuture",
  "-Xlint:missing-interpolator",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-dead-code",
  "-Ywarn-unused"
)

//The default SBT testing java options are too small to support running many of the tests
// due to the need to launch Spark in local mode.
javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")
parallelExecution in Test := false

lazy val spark = "1.6.3"
//lazy val spark = "2.1.0"
lazy val geomesa = "1.3.1-SNAPSHOT"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % spark % "provided",
  "org.apache.spark" %% "spark-sql" % spark % "provided",
  "org.apache.spark" %% "spark-hive" % spark % "provided",
  "org.locationtech.geomesa" %% "geomesa-spark-sql" % geomesa, // keep for convenience of kryo registrator
  "org.locationtech.geomesa" %% "geomesa-cqengine" % geomesa, // for local in memory solution
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"

)

run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in(Compile, run), runner in(Compile, run))

assemblyMergeStrategy in assembly := {
  case PathList("com", "esotericsoftware", xs@_*) => MergeStrategy.last
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.first
}

test in assembly := {}

initialCommands in console :=
  """
    |import com.vividsolutions.jts.geom.Point
    |import org.apache.spark.sql.hive.HiveContext
    |import org.apache.spark.{ SparkConf, SparkContext }
    |import org.apache.spark.sql.{ Row, SQLContext, SaveMode }
    |import org.geotools.factory.CommonFactoryFinder
    |import org.geotools.feature.simple.SimpleFeatureBuilder
    |import org.locationtech.geomesa.spark.GeoMesaSparkKryoRegistrator
    |import org.locationtech.geomesa.utils.text.WKTUtils
    |import org.opengis.filter.Filter
    |import org.geotools.filter.text.ecql.ECQL
    |import org.locationtech.geomesa.utils.geotools.SimpleFeatureTypes
    |import org.opengis.feature.simple.{ SimpleFeature, SimpleFeatureType }
    |import org.locationtech.geomesa.memory.cqengine.GeoCQEngine
    |import collection.JavaConversions._
    |import java.sql.Date
    |
    |val conf: SparkConf = new SparkConf()
    |    .setAppName("geomesaSparkInMemory")
    |    .setMaster("local[*]")
    |    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    |    .set("spark.kryo.registrator", classOf[GeoMesaSparkKryoRegistrator].getName)
    |    .registerKryoClasses(Array(
    |      classOf[Point], classOf[SimpleFeature]
    |    ))
    |  //    .set("spark.sql.crossJoin.enabled", "true")
    |
    |  val sp: SparkContext = new SparkContext(conf)
    |  val spark: SQLContext = new SQLContext(sp) // use sql context as long as hive metastore stuff or complex queries are not required (for 1.6)
    |  //  val spark: HiveContext = new HiveContext(sp)
    |
    |import spark.implicits._
  """.stripMargin

mainClass := Some("myOrg.ExampleSQL")
