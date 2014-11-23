sbtPlugin := true

// Project name (artifact name in Maven)
name := "ebean-jdk8-fix"

// orgnization name (e.g., the package name of the project)
organization := "com.beowulfe.play"

version := "3.3.1-SNAPSHOT"

scalaVersion := "2.10.4"

// project description
description := "Fixes ebean for jdk8"

publishMavenStyle := false

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

// JavaDoc compilation fails because of #link to external project
sources in doc in Compile := List() 
