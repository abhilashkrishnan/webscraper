name := """webscraper"""
organization := "com.webscrapper"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.11"

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.5.14"
libraryDependencies += "org.jsoup" % "jsoup" % "1.10.2"


// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.webscrapper.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.webscrapper.binders._"
