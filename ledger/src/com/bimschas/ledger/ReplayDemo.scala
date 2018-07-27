package com.bimschas.ledger

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.Path

import com.bimschas.ledger.ReplayDemo.initialBalance

// =====================================================================================================================
// Domain model
// =====================================================================================================================

case class Euro(amount: Double) {
  def +(other: Euro): Euro = Euro(amount + other.amount)
  def -(other: Euro): Euro = Euro(amount - other.amount)
}

sealed trait BookingEvent {
  def description: String
}
case class IncomingBookingReceived(amount: Euro, description: String) extends BookingEvent
case class OutgoingBookingReceived(amount: Euro, description: String) extends BookingEvent

case class Balance(amount: Euro) {
  def process(bookingEvent: BookingEvent): Balance = {
    bookingEvent match {
      case IncomingBookingReceived(bookingAmount, _) => Balance(amount + bookingAmount)
      case OutgoingBookingReceived(bookingAmount, _) => Balance(amount - bookingAmount)
    }
  }
}
object Balance {
  def zero: Balance = Balance(amount = Euro(0.0))
}

// =====================================================================================================================
// Replay Demo
// =====================================================================================================================

object Common {

  val eventLog: List[BookingEvent] =
    List(
      IncomingBookingReceived(amount = Euro(50.0), description = "new customer welcome bonus"),
      OutgoingBookingReceived(amount = Euro(30.0), description = "party time!"),
      OutgoingBookingReceived(amount = Euro(300.0), description = "rent"),
      IncomingBookingReceived(amount = Euro(120.0), description = "bad paying weekend promotion job")
    )

  def replay(initialBalance: Balance, eventLog: List[BookingEvent]): Balance =
    eventLog.foldLeft(initialBalance) {
      case (balance, bookingEvent) =>
        balance.process(bookingEvent)
    }
}

object ReplayDemo extends App {
  import Common._

  val initialBalance = Balance.zero
  println(s"initial balance before replay = $initialBalance")

  val balanceAfterReplay = replay(initialBalance, eventLog)
  println(s"balance after replay: $balanceAfterReplay")
}

// =====================================================================================================================
// Snapshot + Replay Demo
// =====================================================================================================================

trait SnapshotStore {
  def persist(snapshot: Balance, path: Path): Unit
  def load(path: Path): Balance
}

object ReplayWithSnapshotDemo extends App {
  import Common._

  val initialBalance = Balance.zero
  println(s"initial balance before replay: $initialBalance")

  val (eventsUntilSnapshot, eventsAfterSnapshot) = Common.eventLog.splitAt(2)

  println(s"simulating a normal business day processing events...")
  val snapshotBalance = replay(initialBalance, eventsUntilSnapshot)
  println(s"balance when taking snapshot: $snapshotBalance")

  val snapshotFilePath = Files.createTempFile("snapshot-replay-demo", "")
  val snapshotStore = JavaSerializationSnapshotStore // really, never ever use Java serialization (other than for demo purposes)!!!
  snapshotStore.persist(snapshotBalance, snapshotFilePath)

  println()
  println(s"... some time later ...")
  println()

  println(s"loading snapshot...")
  val balanceAfterLoadingSnapshot = snapshotStore.load(snapshotFilePath)
  println(s"balance after loading snapshot: $balanceAfterLoadingSnapshot")

  val balanceAfterReplay = replay(balanceAfterLoadingSnapshot, eventsAfterSnapshot)
  println(s"balance after replay: $balanceAfterReplay")
}

// really, never ever use Java serialization (other than for demo purposes)!!!
object JavaSerializationSnapshotStore extends SnapshotStore {

  def persist(snapshot: Balance, path: Path): Unit = {
    // really, never ever use Java serialization (other than for demo purposes)!!!
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val outputStream = new ObjectOutputStream(byteArrayOutputStream)
    outputStream.writeObject(snapshot)
    outputStream.flush()
    try Files.write(path, byteArrayOutputStream.toByteArray)
    finally outputStream.close()
  }

  def load(path: Path): Balance = {
    // really, never ever use Java serialization (other than for demo purposes)!!!
    val bytes = Files.readAllBytes(path)
    val byteArrayInputStream = new ByteArrayInputStream(bytes)
    val objectInputStream = new ObjectInputStream(byteArrayInputStream)
    try objectInputStream.readObject().asInstanceOf[Balance]
    finally objectInputStream.close()
  }
}