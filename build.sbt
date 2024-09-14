// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
val Fs2Version = "3.11.0"
val Http4sVersion = "0.23.27"
val CirceVersion = "0.14.9"
val MunitVersion = "1.0.0"
val LogbackVersion = "1.5.6"
val MunitCatsEffectVersion = "2.0.0"
ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / organization := "dev.hnaderi"
ThisBuild / organizationName := "Hossein Naderi"
ThisBuild / startYear := Some(2023)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("hnaderi", "Hossein Naderi")
)

ThisBuild / scalaVersion := "3.3.3"

lazy val root = tlCrossRootProject.aggregate(domain, catsEffect, zio)

lazy val domain = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .enablePlugins(NoPublishPlugin)
  .settings(
    name := "edomata-domain-example",
    libraryDependencies ++= Seq(
      "dev.hnaderi" %%% "edomata-skunk-circe" % "0.12.3",
      "dev.hnaderi" %%% "edomata-munit" % "0.12.3" % Test,
      "io.circe" %%% "circe-generic" % "0.14.8"
    )
  )

lazy val catsEffect = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .dependsOn(domain)
  .enablePlugins(NoPublishPlugin)
  .settings(
    name := "edomata-ce-example",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-ember-server" % Http4sVersion,
      "org.http4s"      %% "http4s-ember-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "co.fs2" %%% "fs2-core" % Fs2Version,
      "org.scalameta"   %% "munit"               % MunitVersion           % Test,
      "org.typelevel"   %% "munit-cats-effect"   % MunitCatsEffectVersion % Test,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion         % Runtime,
    )
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )

lazy val zio = project
  .dependsOn(domain.jvm)
  .settings(
    name := "edomata-zio-example",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-interop-cats" % "23.1.0.3"
    )
  )
  .enablePlugins(NoPublishPlugin)
