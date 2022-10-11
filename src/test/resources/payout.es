//import special.collection.Coll
//import special.sigma.Box
//
//val SELF: Box
//val OUTPUTS: Coll[Box]
//val INPUTS: Coll[Box]
//val getVar: Int => Option[Int]

{
  val token = SELF.tokens(0)
  val newPayoutBox = OUTPUTS(0)
  val numWinners = SELF.R5[Long].get
  val payoutTotal = SELF.R6[Long].get

  val isExchange =
    newPayoutBox.tokens(0)._1 == token._1 &&
      newPayoutBox.tokens(0)._2 == token._2 + 1 &&
      newPayoutBox.value >= SELF.value - (payoutTotal / numWinners) &&
      newPayoutBox.R4[Long].get == SELF.R4[Long].get &&
      newPayoutBox.R5[Long].get == SELF.R5[Long].get &&
      newPayoutBox.R7[Long].get == SELF.R7[Long].get &&
      newPayoutBox.R6[Long].get == SELF.R6[Long].get

  sigmaProp(isExchange)
}