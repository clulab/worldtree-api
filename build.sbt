name := "worldtree-api"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.github.tototoshi" %% "scala-csv" % "1.2.2",
  "org.clulab" %% "processors" % "5.8.6",
  "org.clulab" %% "processors" % "5.8.6" classifier "models",
  "edu.mit" % "jwi" % "2.2.3",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "junit" % "junit" % "4.10" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "ch.qos.logback" % "logback-classic" % "1.0.10",
  "org.json" % "json" % "20090211",
  "postgresql" % "postgresql" % "9.0-801.jdbc4",
  "xom" % "xom" % "1.2.5",
  "joda-time" % "joda-time" % "2.0",
  "org.joda" % "joda-convert" % "1.1",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.5.1"

)
