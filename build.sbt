import sbt.Project.projectToRef

val scalaV = "2.12.1"

val ammV = "0.8.1"

scalaOrganization in ThisBuild := "org.typelevel"

scalaVersion in ThisBuild := scalaV

classpathTypes += "maven-plugin"

resolvers += Resolver.sonatypeRepo("releases")

// addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

// addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.3" cross CrossVersion.binary)

lazy val jsProjects = Seq(client)

lazy val baseSettings = Seq(version := "0.8",
                            organization := "in.ernet.iisc.math",
                            scalaVersion := scalaV)

lazy val commonSettings = baseSettings ++ Seq(
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies ++= Seq(
      // "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.5",
//      "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
      "org.spire-math" %% "spire"         % "0.13.0",
      "com.lihaoyi"    %% "fansi"         % "0.2.3",
      "com.lihaoyi"    %% "upickle"       % "0.4.4",
      "com.chuusai"    %% "shapeless"     % "2.3.2",
      "org.typelevel"  %% "cats"          % "0.9.0",
      "com.lihaoyi"    % "ammonite"       % ammV cross CrossVersion.full
    ),
    scalacOptions ++= Seq("-unchecked",
                          "-deprecation",
                          "-feature",
                          "-language:existentials")
  )

val akkaV = "2.4.14"

assemblyMergeStrategy in assembly := {
  case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
}

lazy val jvmSettings = Seq(
  libraryDependencies ++= Seq(
    "com.lihaoyi"            % "ammonite"       % ammV % "test" cross CrossVersion.full,
    "com.lihaoyi"            %% "ammonite-ops"  % ammV,
    "com.github.nscala-time" %% "nscala-time"   % "2.16.0",
    "org.reactivemongo"      %% "reactivemongo" % "0.12.1",
    "com.typesafe.akka"      %% "akka-actor"    % akkaV,
    "com.typesafe.akka"      %% "akka-slf4j"    % akkaV,
    "de.heikoseeberger"      %% "akka-sse"      % "2.0.0",
    "org.scalactic" %% "scalactic" % "3.0.1",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
//    "ch.qos.logback" % "logback-classic" % "1.0.9",
    "com.typesafe" % "config"  % "1.3.0",
    "org.mongodb"  %% "casbah" % "3.1.1",
//    "org.mongodb.scala" %% "mongo-scala-driver" % "1.0.0",
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http"   % "10.0.3",
    // "com.typesafe.akka" %% "akka-http" % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.3",
//    "com.lihaoyi" %% "upickle" % "0.3.4",
//    "com.lihaoyi" %% "ammonite-ops" % ammV,
//    "com.lihaoyi" %% "ammonite-shell" % ammV,
    // "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
    "org.slf4j"              % "slf4j-api"       % "1.7.16",
    "org.slf4j"   % "slf4j-simple"  %   "1.7.16",
    "com.lihaoyi" %% "pprint" % "0.4.4",
    // Last stable release
    "org.scalanlp" %% "breeze" % "0.13.+"
    // Native libraries are not included by default. add this if you want them (as of 0.7)
    // Native libraries greatly improve performance, but increase jar sizes.
    // It also packages various blas implementations, which have licenses that may or may not
    // be compatible with the Apache License. No GPL code, as best I know.
    // "org.scalanlp" %% "breeze-natives" % "0.12",
    // The visualization library is distributed separately as well.
    // It depends on LGPL code
    // "org.scalanlp" %% "breeze-viz" % "0.12"
  ),
  resources in Compile += (fastOptJS in (client, Compile)).value.data
)

//lazy val logback = "ch.qos.logback" % "logback-classic" % "1.0.9"

lazy val serverSettings = Seq(
  libraryDependencies ++= Seq(
    // "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    // ws,
    // "org.reactivemongo" %% "play2-reactivemongo" % "0.11.2.play24",
    // "com.vmunier" %% "play-scalajs-scripts" % "0.3.0",
    "org.webjars" % "jquery" % "1.11.1"
  ),
  scalaJSProjects := jsProjects,
  pipelineStages := Seq(scalaJSProd),
  initialCommands in console := """import provingground._ ; import HoTT._; /*import pprint.Config.Colors._; import pprint.pprintln*/"""
)

lazy val nlpSettings = Seq(
  libraryDependencies ++= Seq(
    "com.lihaoyi"         % "ammonite"         % ammV % "test" cross CrossVersion.full,
    "com.lihaoyi"         %% "ammonite-ops"    % ammV,
    "edu.stanford.nlp"    % "stanford-corenlp" % "3.6.0",
    "edu.stanford.nlp"    % "stanford-corenlp" % "3.6.0" classifier "models",
    "com.google.protobuf" % "protobuf-java"    % "2.6.1"
  )
)

lazy val digressionSettings = Seq(
  name := "ProvingGround-Digressions",
  libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % akkaV),
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
)

lazy val acSettings = Seq(
  name := "AndrewsCurtis",
  libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % akkaV),
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  initialCommands in console := """import provingground.andrewscurtis._"""
)

lazy val nfSettings = Seq(
  name := "NormalForm",
  libraryDependencies ++= Seq("org.spire-math" %% "spire" % "0.11.0"),
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  initialCommands in console := """import provingground.normalform._ ; import provingground.normalform.NormalForm._"""
)

lazy val client = project
  .settings(
    name := "ProvingGround-JS",
    scalaVersion := scalaV,
    persistLauncher := true,
    persistLauncher in Test := false,
    // sourceMapsDirectories += coreJS.base / "..",
    unmanagedSourceDirectories in Compile := Seq(
      (scalaSource in Compile).value),
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      "com.lihaoyi"  %%% "scalatags"   % "0.6.3",
      "com.lihaoyi"  %%% "upickle"     % "0.4.4"
    )
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(coreJS)

lazy val core = (crossProject.crossType(CrossType.Pure) in file("core"))
  .settings(commonSettings: _*)
  .settings(name := "ProvingGround-Core")
  .settings(
    libraryDependencies ++= Seq(
//      "com.lihaoyi" %%% "upickle" % "0.3.4"
    ))
  .jsConfigure(_ enablePlugins ScalaJSWeb)
//  .jsSettings(sourceMapsBase := baseDirectory.value / "..")

lazy val coreJVM = core.jvm
lazy val coreJS  = core.js

lazy val functionfinder = project
  .settings(commonSettings: _*)
  .settings(name := "ProvingGround-FunctionFinder")
  .dependsOn(coreJVM)

lazy val mizar = project
  .settings(commonSettings: _*)
  .settings(jvmSettings: _*)
  .settings(name := "Mizar-Parser",
            libraryDependencies += "com.lihaoyi" %% "fastparse" % "0.4.1")

val initCommands =
  """import provingground._; import HoTT._; import ammonite.ops._;  import FansiShow._"""

lazy val mantle = (project in file("mantle"))
  .settings(
    name := "ProvingGround-mantle",
    scalaJSProjects := Seq(client),
    pipelineStages in Assets := Seq(scalaJSPipeline)
    //  libraryDependencies += "com.lihaoyi" % "ammonite" % ammV cross CrossVersion.full
  )
  .settings(commonSettings: _*)
  .settings(jvmSettings: _*)
  .
//        settings(serverSettings : _*).
  settings(initialCommands in (Test, console) :=
    s"""ammonite.Main("$initCommands").run() """)
  .dependsOn(coreJVM)
  .dependsOn(functionfinder)
  // .dependsOn(translation)
  .settings(tutSettings)
  .enablePlugins(SbtWeb)
//        dependsOn(deepwalk).
//        dependsOn(exploring)

lazy val exploring = project
  .settings(name := "ProvingGround-exploring",
            libraryDependencies += "com.lihaoyi" %% "ammonite-ops" % ammV)
  .dependsOn(coreJVM)
  .dependsOn(mantle)

lazy val nlp = (project in file("nlp"))
  .settings(name := "ProvingGround-NLP")
  .settings(commonSettings: _*)
  .settings(nlpSettings: _*)
  .
//        settings(jvmSettings : _*).
//        settings(serverSettings : _*).
  settings(initialCommands in (Test, console) :=
    s"""ammonite.Main("import scala.collection.JavaConversions._, provingground._, PennTrees._, cats._, cats.implicits._, Functors._, SubTypePattern._, TreeToMath._, StanfordParser._").run() """)
  .dependsOn(coreJVM)

// lazy val translation = (project in file("translation"))
//   .settings(name := "ProvingGround-Translation",
//             libraryDependencies += "com.lihaoyi" %% "ammonite-ops" % ammV)
//   .settings(baseSettings: _*)
//   .dependsOn(coreJVM)

lazy val deepwalk = (project in file("deepwalk"))
  .settings(
    name := "DeepWalk4s",
    classpathTypes += "maven-plugin",
    libraryDependencies ++= Seq(
      "org.deeplearning4j" % "deeplearning4j-core"  % "0.7.2",
      "org.deeplearning4j" % "deeplearning4j-graph" % "0.7.2",
      "org.nd4j"           % "nd4j-native-platform" % "0.7.2",
      "org.deeplearning4j" % "deeplearning4j-nlp"   % "0.7.2",
//              "org.deeplearning4j" % "deeplearning4j-ui" % "0.7.2",
      "org.nd4j"    % "nd4j-native"   % "0.7.2",
      "com.lihaoyi" % "ammonite"      % ammV % "test" cross CrossVersion.full,
      "com.lihaoyi" %% "ammonite-ops" % ammV
    )
  )
  .settings(baseSettings: _*)
  .settings(initialCommands in (Test, console) :=
    s"""ammonite.Main("import scala.collection.JavaConversions._").run() """)

// lazy val playServer = (project in file("play-server"))
//   .enablePlugins(PlayScala)
//   .settings(name := "ProvingGround-Play-Server")
//   .settings(commonSettings: _*)
//   .settings(jvmSettings: _*)
//   .settings(serverSettings: _*)
//   .settings(
//     libraryDependencies += specs2 % Test,
//     resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
//     TwirlKeys.templateImports += "controllers._")
//   .aggregate(jsProjects.map(projectToRef): _*)
//   .dependsOn(coreJVM)
//   .dependsOn(functionfinder)
//   .dependsOn(andrewscurtis)
//   .dependsOn(mantle)
//   .dependsOn(nlp)
//   .enablePlugins(SbtWeb)

lazy val realfunctions = (project in file("realfunctions"))
  .settings(commonSettings: _*)
  .settings(jvmSettings: _*)
  .settings(
    //       libraryDependencies  ++= Seq(
    // // other dependencies here
    // //"org.scalanlp" %% "breeze" % "0.11.2",
    // // native libraries are not included by default. add this if you want them (as of 0.7)
    // // native libraries greatly improve performance, but increase jar sizes.
    // //"org.scalanlp" %% "breeze-natives" % "0.11.2"
    // ),
    // resolvers ++= Seq(
    //   // other resolvers here
    //   // if you want to use snapshot builds (currently 0.12-SNAPSHOT), use this.
    //   "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    //   "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
    //   ),
    name := "RealFunctions")

// lazy val digressions = (project in file("digressions"))
//   .settings(commonSettings: _*)
//   .settings(digressionSettings: _*)
//   .dependsOn(coreJVM)
//   .dependsOn(playServer)
//   .dependsOn(functionfinder)

EclipseKeys.skipParents in ThisBuild := false

// unmanagedBase in Compile <<= baseDirectory(_ / "scalalib")

lazy val andrewscurtis = (project in file("andrewscurtis"))
  .settings(commonSettings: _*)
  .settings(jvmSettings: _*)
  .settings(acSettings: _*)
  .dependsOn(coreJVM)
  .dependsOn(mantle)

lazy val normalform = (project in file("normalform"))
  .settings(commonSettings: _*)
  .settings(nfSettings: _*)

fork in run := true
