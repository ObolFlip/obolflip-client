//
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
  val newFlipBox = OUTPUTS(0)
  val selfData = SELF.R7[Coll[Long]]
  val roundNumber = selfData.get(0)
  val roundState = selfData.get(1)
  val ticketsToSell = selfData.get(2)
  val maxTime = selfData.get(3)
  val ticketPrice = selfData.get(4)
  val headsSold = selfData.get(5)
  val tailsSold = selfData.get(6)
  val newFlipData = newFlipBox.R7[Coll[Long]]

  val nextIterationValid = {
    newFlipBox.tokens(0) == SELF.tokens(0) && // marker token
      newFlipBox.propositionBytes == SELF.propositionBytes
  }

  val dataPreserved =
    newFlipData.get(0) == roundNumber &&
      newFlipData.get(1) == roundState &&
      newFlipData.get(2) == ticketsToSell &&
      newFlipData.get(3) == maxTime &&
      newFlipData.get(4) == ticketPrice

  val isTicketSell = {
    newFlipBox.value >= SELF.value + ticketPrice &&
      roundState == 1 &&
      dataPreserved &&
      (CONTEXT.preHeader.timestamp < maxTime || headsSold == 0 || tailsSold == 0) &&
      // we don't check max amount here - the round can end when max amount is reached, but no
      // problem to sell some more tickets
      newFlipBox.tokens(1)._1 == SELF.tokens(1)._1 &&
      newFlipBox.tokens(2)._1 == SELF.tokens(2)._1 &&
      newFlipBox.tokens(1)._2 > 1 && newFlipBox.tokens(2)._2 > 1 &&
      (newFlipBox.tokens(1)._2 == SELF.tokens(1)._2 - 1 &&
        newFlipBox.tokens(2)._2 == SELF.tokens(2)._2 &&
        newFlipData.get(5) == headsSold + 1 &&
        newFlipData.get(6) == tailsSold
        || newFlipBox.tokens(2)._2 == SELF.tokens(2)._2 - 1 &&
        newFlipBox.tokens(1)._2 == SELF.tokens(1)._2 &&
        newFlipData.get(5) == headsSold &&
        newFlipData.get(6) == tailsSold + 1)
  }

  val isNextInit = {
    val currentTime: Long = CONTEXT.preHeader.timestamp
    newFlipBox.value >= SELF.value / 100 &&
      (CONTEXT.preHeader.timestamp > maxTime || headsSold + tailsSold >= ticketsToSell) &&
      headsSold > 0 && tailsSold > 0 &&
      newFlipData.get(0) == roundNumber + 1 &&
      newFlipData.get(1) == 0 &&
      newFlipData.get(2) == configTicketsToSell &&
      newFlipData.get(3) >= currentTime + configMaxTime &&
      newFlipData.get(4) == configTicketPrice &&
      newFlipData.get(5) == 0 &&
      newFlipData.get(6) == 0 &&
      newFlipBox.tokens(1)._1 == SELF.id &&
      newFlipBox.tokens(1)._2 > configTicketsToSell &&
      newFlipBox.R6[Coll[Byte]].get.size == 1 &&
      newFlipBox.R6[Coll[Byte]].get(0) == 48 && // char(48) = '0'
      newFlipBox.R4[Coll[Byte]].get.slice(0, configHeadPrefix.size) == configHeadPrefix && // "ObolFlip Heads Token #"
      OUTPUTS(1).value >= SELF.value * 98 / 100 &&
      OUTPUTS(1).R4[Long].get == roundNumber &&
      OUTPUTS(1).R5[Long].get == maxTime + configCooldown &&
      OUTPUTS(1).R6[Coll[Long]].get(0) == headsSold &&
      OUTPUTS(1).R6[Coll[Long]].get(1) == tailsSold &&
      blake2b256(OUTPUTS(1).propositionBytes) == cooldownBoxHash
  }

  val isDoneInit = {
    newFlipBox.value >= 1000000 &&
      newFlipData.get(0) == roundNumber &&
      newFlipData.get(1) == 1 &&
      newFlipData.get(2) == ticketsToSell &&
      newFlipData.get(3) == maxTime &&
      newFlipData.get(4) == ticketPrice &&
      newFlipData.get(5) == 0 &&
      newFlipData.get(6) == 0 &&
      newFlipBox.tokens(2)._1 == SELF.id &&
      newFlipBox.tokens(2)._2 > ticketsToSell &&
      newFlipBox.R6[Coll[Byte]].get.size == 1 &&
      newFlipBox.R6[Coll[Byte]].get(0) == 48 && // char(48) = '0'
      newFlipBox.R4[Coll[Byte]].get.slice(0, configTailPrefix.size) == configTailPrefix // "ObolFlip Tails Token #"
  }

  val isUpdate = INPUTS(0).tokens(0)._1 == updateNFT

  sigmaProp((isTicketSell || isNextInit || isDoneInit) && nextIterationValid || isUpdate)
}