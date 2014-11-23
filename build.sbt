// Project name (artifact name in Maven)
name := "ebean-jdk8-fix"

// orgnization name (e.g., the package name of the project)
organization := "com.beowulfe.play"

version := "3.3.1-SNAPSHOT"

// project description
description := "Fixes ebean for jdk8"

// Enables publishing to maven repo
publishMavenStyle := true

// Do not append Scala versions to the generated artifacts
crossPaths := false

// This forbids including Scala related libraries into the dependency
autoScalaLibrary := false

// library dependencies. (orginization name) % (project name) % (version)
libraryDependencies ++= Seq(
   "org.apache.ant" % "ant" % "1.7.0",
   "javax.persistence" % "persistence-api" % "1.0"
)

packageOptions := Seq(
  Package.ManifestAttributes("Premain-Class" -> "com.avaje.ebean.enhance.agent.Transformer"),
  Package.ManifestAttributes("Agent-Class" -> "com.avaje.ebean.enhance.agent.Transformer"),
  Package.ManifestAttributes("Can-Redefine-Classes" -> "true"),
  Package.ManifestAttributes("Can-Retransform-Classes" -> "true")
)

sources in doc in Compile := List() 
