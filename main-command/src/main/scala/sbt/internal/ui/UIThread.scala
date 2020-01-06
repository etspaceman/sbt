/*
 * sbt
 * Copyright 2011 - 2018, Lightbend, Inc.
 * Copyright 2008 - 2010, Mark Harrah
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package sbt.internal.ui

import java.io.File
import java.nio.channels.ClosedChannelException
import java.util.concurrent.atomic.AtomicBoolean

import jline.console.history.PersistentHistory
import sbt.BasicKeys.{ historyPath, newShellPrompt }
import sbt.State
import sbt.internal.util.complete.{ JLineCompletion, Parser }
import sbt.internal.util.{ ConsoleAppender, LineReader, ProgressEvent, Prompt, Terminal }

import scala.annotation.tailrec

trait UIThread extends Thread with AutoCloseable { self: Thread =>
  private[sbt] def reader: UIThread.Reader
  private[sbt] def handleInput(s: Either[String, String]): Boolean
  private[this] val isStopped = new AtomicBoolean(false)
  private[sbt] def onProgressEvent(pe: ProgressEvent, terminal: Terminal): Unit
  self.setDaemon(true)
  self.start()
  override abstract def run(): Unit = {
    @tailrec def impl(): Unit = {
      val res = reader.readLine()
      if (!handleInput(res) && !isStopped.get) impl()
    }
    try impl()
    catch { case _: InterruptedException | _: ClosedChannelException => isStopped.set(true) }
  }
  override def close(): Unit = {
    isStopped.set(true)
    interrupt()
  }
}

object UIThread {
  trait Reader { def readLine(): Either[String, String] }
  object Reader {
    def terminalReader(prompt: Prompt, parser: Parser[_])(
        terminal: Terminal,
        state: State
    ): Reader = {
      val lineReader = LineReader.createReader(history(state), terminal, prompt)
      JLineCompletion.installCustomCompletor(lineReader, parser)
      () => {
        import ConsoleAppender._
        def clear(): Unit = if (terminal.isAnsiSupported) {
          val ps = terminal.printStream
          ps.print(DeleteLine + clearScreen(0) + CursorLeft1000)
          ps.flush()
        }
        clear()
        try {
          terminal.setPrompt(prompt)
          val res = lineReader.readLine(prompt.mkPrompt())
          terminal.printStream.write(Int.MinValue)
          terminal.setPrompt(Prompt.Running)
          res match {
            case null => Left("kill channel")
            case s: String =>
              lineReader.getHistory match {
                case p: PersistentHistory =>
                  p.add(s)
                  p.flush()
                case _ =>
              }
              Right(s)
          }
        } catch {
          case _: InterruptedException => Right("")
        } finally lineReader.close()
      }
    }
  }
  private[this] def history(s: State): Option[File] =
    s.get(historyPath).getOrElse(Some(new File(s.baseDir, ".history")))
  private[sbt] def shellPrompt(terminal: Terminal, s: State): String =
    s.get(newShellPrompt) match {
      case Some(pf) => pf(terminal, s)
      case None =>
        def ansi(s: String): String = if (terminal.isAnsiSupported) s"$s" else ""
        s"${ansi(ConsoleAppender.DeleteLine)}> ${ansi(ConsoleAppender.clearScreen(0))}"
    }
}
