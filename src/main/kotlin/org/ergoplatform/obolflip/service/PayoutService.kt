package org.ergoplatform.obolflip.service

import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.impl.InputBoxImpl
import org.ergoplatform.obolflip.Config
import org.ergoplatform.obolflip.Constants
import org.ergoplatform.obolflip.handler.PayoutBox
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PayoutService(
    private val explorerApiService: ExplorerApiService,
    private val nodePeerService: NodePeerService,
    private val config: Config,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val payoutBoxesMap = HashMap<Int, PayoutBox>()
    private var blockLoaded = 0
    private var updateTimeMs = 0L
    private var payoutBoxesList = emptyList<PayoutBox>()

    fun redeem(address: String, roundNumber: Int): ReducedTransaction {
        updatePayoutMap()

        val payoutBox = payoutBoxesMap[roundNumber]!!

        val redeemAddress = Address.create(address)
        try {
            return nodePeerService.getErgoClient().execute { ctx ->
                val txFeeAmount = Parameters.MinFee
                val inputs = BoxOperations.createForSender(redeemAddress, ctx)
                    .withTokensToSpend(listOf(ErgoToken(payoutBox.winningTokenId, 1)))
                    .withAmountToSpend(Parameters.MinChangeValue)
                    .withFeeAmount(txFeeAmount)
                    .withInputBoxesLoader(ExplorerAndPoolUnspentBoxesLoader())
                    .loadTop()
                    .filter { box -> box.tokens.find { it.id == payoutBox.winningTokenId } != null }

                val unsigned = payoutBox.redeemTicket(ctx,
                    redeemAddress,
                    inputs.first(),
                    config.getFeeAddress()?.let {
                        listOf(Pair(70, it), Pair(30, Constants.softwareLicenseFee))
                    } ?: listOf(Pair(100, Constants.softwareLicenseFee)),
                    txFeeAmount,
                )
                ctx.newProverBuilder().build().reduce(unsigned, 0)
            }
        } catch (net: InputBoxesSelectionException.NotEnoughTokensException) {
            throw IllegalStateException("Winning token for round not found")
        } catch (t: Throwable) {
            logger.error("Error redeeming", t)
            throw t
        }
    }

    private fun updatePayoutMap() {
        if (System.currentTimeMillis() - updateTimeMs > 1000L * 60) synchronized(this) {
            logger.debug("Updating payout boxes")
            try {
                var oldestBoxFound = Integer.MAX_VALUE
                var newestBoxFound = 0
                var page = 0
                val pageSize = 20
                var allLoaded = false

                while (oldestBoxFound >= blockLoaded && !allLoaded && page <= 5) {
                    explorerApiService.getBoxesByAddress(
                        Constants.payoutAddrString,
                        page * pageSize,
                        pageSize,
                        false
                    ).items?.let { itemsLoaded ->
                        allLoaded = itemsLoaded.isEmpty()

                        if (!allLoaded) {
                            oldestBoxFound = itemsLoaded.last().settlementHeight
                            if (page == 0)
                                newestBoxFound = itemsLoaded.first().settlementHeight
                        }

                        itemsLoaded.forEach { box ->
                            try {
                                nodePeerService.getBoxDataById(box.boxId)?.let {
                                    val payoutBoxHandler = PayoutBox(InputBoxImpl(it))
                                    payoutBoxesMap[payoutBoxHandler.roundNumber.toInt()] =
                                        payoutBoxHandler

                                    logger.debug("Updating payout box round ${payoutBoxHandler.roundNumber}")
                                }
                            } catch (t: Throwable) {
                                // ignore errors with a single box
                            }
                        }
                    }

                    page++
                }

                blockLoaded = newestBoxFound
            } catch (t: Throwable) {
                logger.error("Error updating payout boxes", t)
            }

            updateTimeMs = System.currentTimeMillis()
            payoutBoxesList = payoutBoxesMap.values.sortedByDescending { it.roundNumber }
        }
    }

    fun getPayoutBoxesList(): List<PayoutBox> {
        updatePayoutMap()
        return payoutBoxesList
    }
}