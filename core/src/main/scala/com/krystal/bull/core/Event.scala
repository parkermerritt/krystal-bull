package com.krystal.bull.core

import com.krystal.bull.core.storage.{EventDb, EventOutcomeDb}
import org.bitcoins.crypto.{FieldElement, SchnorrDigitalSignature, SchnorrNonce}

trait Event {
  def nonce: SchnorrNonce
  def label: String
  def numOutcomes: Long
  def signingVersion: SigningVersion
  def outcomes: Vector[String]
}

case class PendingEvent(
    nonce: SchnorrNonce,
    label: String,
    numOutcomes: Long,
    signingVersion: SigningVersion,
    outcomes: Vector[String])
    extends Event

case class CompletedEvent(
    nonce: SchnorrNonce,
    label: String,
    numOutcomes: Long,
    signingVersion: SigningVersion,
    outcomes: Vector[String],
    attestation: FieldElement)
    extends Event {

  val signature: SchnorrDigitalSignature =
    SchnorrDigitalSignature(nonce, attestation)
}

object Event {

  def apply(eventDb: EventDb, outcomeDbs: Vector[EventOutcomeDb]): Event = {
    val outcomes = outcomeDbs.map(_.message)

    eventDb.attestationOpt match {
      case Some(sig) =>
        CompletedEvent(eventDb.nonce,
                       eventDb.label,
                       eventDb.numOutcomes,
                       eventDb.signingVersion,
                       outcomes,
                       sig)
      case None =>
        PendingEvent(eventDb.nonce,
                     eventDb.label,
                     eventDb.numOutcomes,
                     eventDb.signingVersion,
                     outcomes)
    }
  }
}
