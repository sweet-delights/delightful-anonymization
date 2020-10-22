import java.util.regex.Pattern
import sbt.Keys.scalacOptions

name := "delightful-anonymization"
organization := "sweet-delights"
scalaVersion := "2.12.12"
crossScalaVersions := Seq("2.12.12", "2.13.3")
checksums in update := Nil
libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
  "commons-codec"  % "commons-codec" % "1.9",
  "com.chuusai"    %% "shapeless"    % "2.3.3",
  "org.specs2"     %% "specs2-core"  % "4.5.1" % "test"
)
scalacOptions ++= Seq(
  "-deprecation",
  "-target:jvm-1.8",
  "-feature"
)
javacOptions in Compile ++= Seq(
  "-source",
  "1.8",
  "-target",
  "1.8"
)
scalafmtOnCompile in ThisBuild := true

// sbt-release
import sbtrelease._
import ReleaseTransformations._
releaseCrossBuild := true
releaseVersion := { ver =>
  val bumpedVersion = Version(ver)
    .map { v =>
      suggestedBump.value match {
        case Version.Bump.Bugfix => v.withoutQualifier.string
        case _ => v.bump(suggestedBump.value).withoutQualifier.string
      }
    }
    .getOrElse(versionFormatError(ver))
  bumpedVersion
}
releaseNextVersion := { ver =>
  Version(ver).map(_.withoutQualifier.bump.string).getOrElse(versionFormatError(ver)) + "-SNAPSHOT"
}
releaseCommitMessage := s"[sbt-release] setting version to ${(version in ThisBuild).value}"
bugfixRegexes := List(s"${Pattern.quote("[patch]")}.*").map(_.r)
minorRegexes := List(s"${Pattern.quote("[minor]")}.*").map(_.r)
majorRegexes := List(s"${Pattern.quote("[major]")}.*").map(_.r)
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  runClean,
  runTest,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
