//import special.collection.Coll
//import special.sigma.{Box, Context, PreHeader}
//
//val SELF: Box
//val OUTPUTS: Coll[Box]
//val INPUTS: Coll[Box]
//val getVar: Int => Option[Int]
//def byteArrayToBigInt(input: Coll[Byte]): BigInt
//def blake2b256(input: Coll[Byte]): Coll[Byte]
//val CONTEXT: Context

{
  val payoutBox = OUTPUTS(0)
  val randomBox = CONTEXT.dataInputs(0)
  val flipWin = byteArrayToBigInt(randomBox.id.slice(0, 15)) % 2
  val winnerToken = if (flipWin == 0) SELF.tokens(0) else SELF.tokens(1)
  val winnerNum = SELF.R6[Coll[Long]].get(if (flipWin == 0) 0 else 1)

  val isValidCoolDownAndPayout = {
    CONTEXT.preHeader.timestamp > SELF.R5[Long].get &&
      payoutBox.R4[Long].get == SELF.R4[Long].get &&
      payoutBox.value >= SELF.value - SELF.value / 100 &&
      payoutBox.R6[Long].get == payoutBox.value &&
      payoutBox.R5[Long].get == winnerNum &&
      payoutBox.R7[Long].get == (if (flipWin == 0) 0L else 1L) &&
      payoutBox.tokens(0) == winnerToken &&
      randomBox.tokens(0)._1 == randomBoxToken &&
      blake2b256(payoutBox.propositionBytes) == payoutBoxHash
  }

  sigmaProp(isValidCoolDownAndPayout)
}