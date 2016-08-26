enablePlugins(ScalaJSPlugin)

name := "RxScalaJsSamples"

version := "1.0"

scalaVersion := "2.11.8"


resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "com.github.lukajcb" %%% "rxscala-js" % "0.1.8-SNAPSHOT",
  "org.scala-js" %%% "scalajs-dom" % "0.9.0"
)

jsDependencies ++= Seq(
  "org.webjars.npm" % "rxjs" % "5.0.0-beta.11" / "Rx.umd.min.js" commonJSName "Rx"
)
