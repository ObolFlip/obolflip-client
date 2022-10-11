package org.ergoplatform.obolflip.handler

import org.ergoplatform.appkit.*
import org.ergoplatform.obolflip.Constants
import special.collection.Coll
import java.math.BigInteger
import kotlin.math.abs

class CoolDownOperations(
    val cooldownBox: InputBox
) {
    val receipt = cooldownBox.value / 100L
    val roundNumber = cooldownBox.registers[0].value as Long
    val transitioningTime = cooldownBox.registers[1].value as Long
    val headsSold = (cooldownBox.registers[2].value as Coll<*>).apply(0) as Long
    val tailsSold = (cooldownBox.registers[2].value as Coll<*>).apply(1) as Long

    fun transitionToPayout(
        ctx: BlockchainContext,
        dataInput: InputBox,
        revenueAddress: Address,
        feeAmount: Long = Parameters.MinFee,
    ): UnsignedTransaction {
        val bigInteger = BigInteger(dataInput.id.bytes.take(15).toByteArray())
        val dataInputWinner = abs(bigInteger.mod(BigInteger.TWO).toInt())

        val txB = ctx.newTxBuilder()

        val payoutBox = txB.outBoxBuilder()
            .value(cooldownBox.value - receipt)
            .contract(Constants.payoutAddress.toErgoContract())
            .tokens(cooldownBox.tokens[dataInputWinner])
            .registers(
                ErgoValue.of(roundNumber),
                ErgoValue.of(
                    (cooldownBox.registers[2].value as Coll<*>).apply(dataInputWinner) as Long
                ),
                ErgoValue.of(cooldownBox.value - receipt),
                ErgoValue.of(dataInputWinner.toLong()),
            )
            .build()

        val receipt = receipt - feeAmount
        val softwareFeeBox = txB.outBoxBuilder()
            .contract(Constants.softwareLicenseFee.toErgoContract())
            .value(receipt * 30 / 100)
            .build()

        val receiptBox = txB.outBoxBuilder()
            .value(receipt - softwareFeeBox.value)
            .contract(revenueAddress.toErgoContract())
            .build()

        return ctx.newTxBuilder()
            .boxesToSpend(listOf(cooldownBox))
            .sendChangeTo(revenueAddress.ergoAddress)
            .withDataInputs(listOf(dataInput))
            .fee(feeAmount)
            .outputs(payoutBox, receiptBox, softwareFeeBox)
            .tokensToBurn(cooldownBox.tokens[(dataInputWinner + 1) % 2])
            .build()
    }
}