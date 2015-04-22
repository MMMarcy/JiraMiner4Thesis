name := "JIRAMiner"

version := "1.0"

scalaVersion := "2.11.6"

scalacOptions ++= List("-deprecation", "-feature")


libraryDependencies += "io.spray" %%  "spray-json" % "1.3.1"
libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.2.1"
    