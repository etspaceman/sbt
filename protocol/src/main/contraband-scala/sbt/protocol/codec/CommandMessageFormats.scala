/**
 * This code is generated using [[https://www.scala-sbt.org/contraband/ sbt-contraband]].
 */

// DO NOT EDIT MANUALLY
package sbt.protocol.codec

import _root_.sjsonnew.JsonFormat
trait CommandMessageFormats { self: sjsonnew.BasicJsonProtocol with sbt.protocol.codec.InitCommandFormats with sbt.protocol.codec.ExecCommandFormats with sbt.protocol.codec.SettingQueryFormats with sbt.protocol.codec.AttachFormats with sbt.protocol.codec.TerminalPropertiesQueryFormats with sbt.protocol.codec.TerminalBooleanCapabilityQueryFormats with sbt.protocol.codec.TerminalNumericCapabilityQueryFormats with sbt.protocol.codec.TerminalStringCapabilityQueryFormats =>
implicit lazy val CommandMessageFormat: JsonFormat[sbt.protocol.CommandMessage] = flatUnionFormat8[sbt.protocol.CommandMessage, sbt.protocol.InitCommand, sbt.protocol.ExecCommand, sbt.protocol.SettingQuery, sbt.protocol.Attach, sbt.protocol.TerminalPropertiesQuery, sbt.protocol.TerminalBooleanCapabilityQuery, sbt.protocol.TerminalNumericCapabilityQuery, sbt.protocol.TerminalStringCapabilityQuery]("type")
}
