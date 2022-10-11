package org.ergoplatform.obolflip.handler

import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.obolflip.Constants
import java.lang.Long.max

class PayoutBox(
    val payoutBox: InputBox
) {
    val receipt =
        (payoutBox.registers[2].value as Long / payoutBox.registers[1].value as Long)
    val winningTokenId = payoutBox.tokens[0].id
    val roundNumber = payoutBox.registers[0].value as Long
    val headsWon = (payoutBox.registers[3].value as Long) == 0L

    fun redeemTicket(
        ctx: BlockchainContext,
        redeemAddress: Address,
        userTokenBox: InputBox,
        feeAddresses: List<Pair<Int, Address>> = emptyList(),
        feeAmount: Long = Parameters.MinFee
    ): UnsignedTransaction {
        val txB = ctx.newTxBuilder()

        val newPayoutBox = txB.outBoxBuilder()
            .value(max(payoutBox.value - receipt, Parameters.MinChangeValue))
            .contract(ErgoTreeContract(payoutBox.ergoTree, Constants.networkType))
            .tokens(ErgoToken(winningTokenId, payoutBox.tokens[0].value + 1))
            .registers(*payoutBox.registers.toTypedArray())
            .build()

        val completeReceipt = (payoutBox.value - newPayoutBox.value) - feeAmount

        val feeBoxes = feeAddresses.map { (percent, address) ->
            txB.outBoxBuilder()
                .contract(address.toErgoContract())
                .value(completeReceipt * percent / 10000)
                .build()
        }

        val receiptBox = txB.outBoxBuilder()
            .value(completeReceipt - feeBoxes.sumOf { it.value })
            .contract(redeemAddress.toErgoContract())
            .build()

        return ctx.newTxBuilder()
            .boxesToSpend(listOf(payoutBox, userTokenBox))
            .sendChangeTo(redeemAddress.ergoAddress)
            .fee(feeAmount)
            .outputs(newPayoutBox, receiptBox, *feeBoxes.toTypedArray())
            .build()
    }
}