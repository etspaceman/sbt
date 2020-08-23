/*
 * sbt
 * Copyright 2011 - 2018, Lightbend, Inc.
 * Copyright 2008 - 2010, Mark Harrah
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package sbt

import java.io.File

import sbt.OutputStrategy._
import sbt.internal.TestLogger
import sbt.io.{ IO, Path }

import org.scalacheck.Gen.{ alphaNumChar, frequency, nonEmptyListOf }
import org.scalacheck.Prop.{ Exception => _, _ }
import org.scalacheck._

object ForkTest extends Properties("Fork") {

  /**
   * Heuristic for limiting the length of the classpath string.
   * Longer than this will hit hard limits in the total space
   * allowed for process initialization, which includes environment variables, at least on linux.
   */
  final val MaximumClasspathLength = 100000

  lazy val genOptionName: Gen[Option[String]] =
    frequency((9, Some("-cp")), (9, Some("-classpath")), (1, None))
  lazy val pathElement: Gen[String] = nonEmptyListOf(alphaNumChar).map(_.mkString)
  lazy val path: Gen[String] = nonEmptyListOf(pathElement).map(_.mkString(File.separator))
  lazy val genRelClasspath: Gen[List[String]] = nonEmptyListOf(path)

  lazy val requiredEntries: List[File] =
    IO.classLocationPath[scala.Option[_]].toFile ::
      IO.classLocationPath[sbt.exit.type].toFile ::
      Nil
  lazy val mainAndArgs: List[String] =
    "sbt.exit" ::
      "0" ::
      Nil

  property("Arbitrary length classpath successfully passed.") =
    forAllNoShrink(genOptionName, genRelClasspath) {
      (optionName: Option[String], relCP: List[String]) =>
        IO.withTemporaryDirectory { dir =>
          TestLogger { log =>
            val withScala = requiredEntries ::: relCP.map(rel => new File(dir, rel))
            val absClasspath = trimClasspath(Path.makeString(withScala))
            val args = optionName.map(_ :: absClasspath :: Nil).toList.flatten ++ mainAndArgs
            val config = ForkOptions().withOutputStrategy(LoggedOutput(log))
            val exitCode = try Fork.java(config, args)
            catch { case e: Exception => e.printStackTrace; 1 }
            val expectedCode = if (optionName.isEmpty) 1 else 0
            s"temporary directory: ${dir.getAbsolutePath}" |:
              s"required classpath: ${requiredEntries.mkString("\n\t", "\n\t", "")}" |:
              s"main and args: ${mainAndArgs.mkString(" ")}" |:
              s"args length: ${args.mkString(" ").length}" |:
              s"exitCode: $exitCode, expected: $expectedCode" |:
              (exitCode == expectedCode)
          }
        }
    }

  private[this] def trimClasspath(cp: String): String =
    if (cp.length > MaximumClasspathLength) {
      val lastEntryI = cp.lastIndexOf(File.pathSeparatorChar.toInt, MaximumClasspathLength)
      if (lastEntryI > 0)
        cp.substring(0, lastEntryI)
      else
        cp
    } else
      cp
}

// Object used in the tests
object exit {
  def main(args: Array[String]): Unit = {
    System.exit(java.lang.Integer.parseInt(args(0)))
  }
}
