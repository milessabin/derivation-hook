import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import org.scalajs.sbtplugin.cross.CrossProject
import ReleaseTransformations._

import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys
import MimaKeys.{mimaPreviousArtifacts, mimaBinaryIssueFilters}
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.ProblemFilters._

lazy val buildSettings = Seq(
  organization := "org.typelevel",
  scalaVersion := "2.12.8",
  crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.0"),
  sourceGenerators in Compile += Def.task(Boilerplate.genCode((sourceManaged in Compile).value)).taskValue
)

def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _ => false
  }

lazy val commonSettings = Seq(
  incOptions := incOptions.value.withLogRecompileOnMacro(false),

  scalacOptions := Seq(
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked"
  ) ++ (if(!priorTo2_13(scalaVersion.value)) Seq("-Ymacro-annotations") else Nil),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    "bintray/non" at "http://dl.bintray.com/non/maven"
  ),
  libraryDependencies ++= Seq(
    "com.chuusai"          %%% "shapeless"     % "2.3.3"  % "test",
    "com.github.mpilquist" %%% "simulacrum"    % "0.19.0" % "test",
    "org.scalatest"        %%% "scalatest"     % "3.0.8"  % "test",
    "org.scalacheck"       %%% "scalacheck"    % "1.14.0" % "test",

    compilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
  ),
  scmInfo :=
    Some(ScmInfo(
      url("https://github.com/milessabin/export-hook"),
      "scm:git:git@github.com:milessabin/export-hook.git"
    ))
) ++ crossVersionSharedSources ++ scalaMacroDependencies

lazy val commonJsSettings = Seq(
  scalaJSStage in Global := FastOptStage,
  parallelExecution in Test := false
)

lazy val commonJvmSettings = Seq(
  parallelExecution in Test := false
)

lazy val coreSettings = buildSettings ++ commonSettings ++ publishSettings

lazy val root = project.in(file("."))
  .aggregate(coreJS, coreJVM)
  .dependsOn(coreJS, coreJVM)
  .settings(coreSettings:_*)
  .settings(noPublishSettings)

lazy val core = crossProject.crossType(CrossType.Pure)
  .settings(moduleName := "export-hook")
  .settings(coreSettings:_*)
  .settings(mimaSettings:_*)
  .jsSettings(commonJsSettings:_*)
  .jvmSettings(commonJvmSettings:_*)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

addCommandAlias("validate", ";root;compile;mimaReportBinaryIssues;test")
addCommandAlias("release-all", ";root;release")
addCommandAlias("js", ";project coreJS")
addCommandAlias("jvm", ";project coreJVM")
addCommandAlias("root", ";project root")

lazy val scalaMacroDependencies: Seq[Setting[_]] = Seq(
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
  ) ++ (if(priorTo2_13(scalaVersion.value)) Seq(
    compilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.patch)) else Nil
  )
)

lazy val crossVersionSharedSources: Seq[Setting[_]] =
  Seq(Compile, Test).map { sc =>
    (unmanagedSourceDirectories in sc) ++= {
      (unmanagedSourceDirectories in sc ).value.map {
        dir:File => new File(dir.getPath + "_" + scalaBinaryVersion.value)
      }
    }
  }

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/milessabin/export-hook")),
  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <developers>
      <developer>
        <id>milessabin</id>
        <name>Miles Sabin</name>
        <url>http://milessabin.com/blog</url>
      </developer>
    </developers>
  )
)

lazy val mimaSettings = mimaDefaultSettings ++ Seq(
  mimaPreviousArtifacts := { Set() },

  mimaBinaryIssueFilters ++= {
    // Filtering the methods that were added since the checked version
    // (these only break forward compatibility, not the backward one)
    Seq(
    )
  }
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val sharedReleaseProcess = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)

credentials ++= (for {
  username <- Option(System.getenv().get("SONATYPE_USERNAME"))
  password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
} yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
