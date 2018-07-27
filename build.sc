// build.sc
import mill._
import mill.scalalib._

object Settings {
  val scalaVersion = "2.12.6"
}

def runReplayDemo = T {
	ledger.runMain("com.bimschas.ledger.ReplayDemo")
}

def runSnapshotDemo = T {
	ledger.runMain("com.bimschas.ledger.ReplayWithSnapshotDemo")
}

object ledger extends ScalaModule {
	def scalaVersion = Settings.scalaVersion
}

