package org.ergoplatform.obolflip.service

import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.impl.InputBoxImpl
import org.ergoplatform.appkit.impl.OutBoxImpl
import org.ergoplatform.obolflip.Config
import org.ergoplatform.obolflip.Constants
import org.ergoplatform.obolflip.formatErgAmount
import org.ergoplatform.obolflip.handler.CoolDownOperations
import org.ergoplatform.obolflip.handler.ObolFlipBoxOperations
import org.ergoplatform.obolflip.millisecondsToRelativeTime
import org.ergoplatform.restapi.client.ErgoTransaction
import org.ergoplatform.restapi.client.ErgoTransactionOutput
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ObolFlipService(
    private val explorerApiService: ExplorerApiService,
    private val nodePeerService: NodePeerService,
    private val config: Config,
) {
    private val logger = LoggerFactory.getLogger(ObolFlipService::class.java)

    private var obolFlipState: ObolFlipBoxOperations? = null
    private var lastStateCheckTs = 0L
    private var coolDownState: CoolDownOperations? = null

    private var feeAddress: Address? = config.getFeeAddress()

    fun getCurrentObolFlipState(): ObolFlipBoxOperations {
        updateObolFlipState()
        return obolFlipState!!
    }

    private fun updateObolFlipState(forced: Boolean = false): Boolean {
        var updated: Boolean = false
        synchronized(this) {
            if (forced || System.currentTimeMillis() - lastStateCheckTs > 1000L * 60) {

                val flipStateBoxValid = obolFlipState?.let {
                    nodePeerService.getBoxDataById(
                        it.currentFlipBox.id.toString(),
                        withPool = true
                    ) != null
                } ?: false

                logger.debug("Updating obol flip state, box valid: $flipStateBoxValid")

                if (!flipStateBoxValid) {
                    val obolAddressBoxes = explorerApiService.getBoxesByAddress(
                        Constants.obolFlipAddrString,
                        0,
                        20,
                        false
                    ).items!!

                    val obolFlipBox = obolAddressBoxes.find { box ->
                        box.assets.find { it.tokenId == Constants.markerNftId } != null
                    } ?: run {
                        logger.warn("Active flip not found on expected address, searching for marker token...")
                        explorerApiService.getBoxesByTokenId(Constants.markerNftId, 0, 1).first()
                    }

                    if (obolFlipBox.boxId != obolFlipState?.currentFlipBox?.id?.toString()) {
                        val boxFromNode = nodePeerService.getBoxDataById(obolFlipBox.boxId)
                        obolFlipState =
                            ObolFlipBoxOperations(InputBoxImpl(boxFromNode), Constants.networkType)
                        updated = true
                    }
                }

                // check mempool
                logger.debug("Checking mempool...")
                val unconfirmedTx = nodePeerService.getUnconfirmedTransactions()
                // search for flip box transaction in mem-pool
                var unconfirmedBoxToUse: ErgoTransactionOutput? = null
                var flipBoxIdToUse = obolFlipState?.currentFlipBox?.id?.toString()
                var foundNext = true

                val isFlipBoxTx: (ErgoTransaction) -> Boolean =
                    { tx -> tx.inputs.any { flipBoxIdToUse == it.boxId } && tx.outputs.firstOrNull()?.ergoTree == obolFlipState?.currentFlipBox?.ergoTree?.bytesHex() }
                while (foundNext && unconfirmedTx?.find(isFlipBoxTx) != null) {
                    // filter any transactions for the flip box...
                    val unconfirmedFlipBox =
                        unconfirmedTx.firstOrNull(isFlipBoxTx)?.outputs?.firstOrNull() // new flip box is always output 0
                    // check to retrieve the box data. it is weird, but the unconfirmed transactions
                    // can contain transactions with outboxes not valid
                    foundNext = unconfirmedFlipBox?.boxId?.let {
                        nodePeerService.getBoxDataById(it, true)
                    } != null
                    if (foundNext) {
                        flipBoxIdToUse = unconfirmedFlipBox?.boxId
                        unconfirmedBoxToUse = unconfirmedFlipBox
                    }
                }

                if (unconfirmedBoxToUse != null) {
                    logger.debug("Found unconfirmed flip transaction with box ${unconfirmedBoxToUse.boxId}")
                    updated = true
                    obolFlipState =
                        ObolFlipBoxOperations(
                            InputBoxImpl(unconfirmedBoxToUse),
                            Constants.networkType
                        )
                } else
                    logger.debug("Checking mempool done.")

                if (updated) {
                    val newFlipState = obolFlipState!!
                    logger.info("Flip state updated, new box ${newFlipState.currentFlipBox.id}")
                    logger.info("Round: ${newFlipState.roundNumber}")
                    if (newFlipState.endTimeMs < System.currentTimeMillis())
                        logger.info("Ends: in ${millisecondsToRelativeTime(newFlipState.endTimeMs)}")
                    else
                        logger.info("Waiting for end")
                    logger.info("Tickets to sell: ${newFlipState.ticketsToSell}")
                    logger.info("Ticket price: ${formatErgAmount(newFlipState.ticketPrice)} ERG")
                    logger.info("Heads sold: ${newFlipState.headsSold}")
                    logger.info("Tails sold: ${newFlipState.tailsSold}")
                }

                // check if a found cooldown is invalid
                coolDownState?.let {
                    if (nodePeerService.getBoxDataById(it.cooldownBox.id.toString(), true) == null)
                        coolDownState = null
                }

                lastStateCheckTs = System.currentTimeMillis()
            }
        }

        return updated
    }

    fun getCoolDownState(): CoolDownOperations? {
        updateObolFlipState()
        return coolDownState
    }

    private var lastManagedBoxId: String? = null
    private var lastManagedMs = 0L

    @Scheduled(fixedDelay = 1000L * 60)
    fun manageFlip() {
        if (!config.manageFlip)
            return

        if (config.manageFlip && feeAddress == null) {
            logger.error("ManageFlip activated, but no fee revenue address set.")
            return
        }

        val flipState = getCurrentObolFlipState()
        val currentFlipBoxId = flipState.currentFlipBox.id.toString()
        if (currentFlipBoxId != lastManagedBoxId || System.currentTimeMillis() - lastManagedMs > 1000L * 60 * Constants.coolDownMinutes / 4) {
            flipState.txFee = config.getTxFee()

            // check flip state and how we can proceed
            try {
                if (flipState.isInitializing) {
                    logger.info("Finishing flip initialisation...")

                    nodePeerService.getErgoClient().execute { ctx ->
                        val unsignedTx = flipState.finalizeInit(ctx, feeAddress!!)
                        val signedTx = ctx.newProverBuilder().build().sign(unsignedTx)
                        ctx.sendTransaction(signedTx)
                        logger.info("Done, submitted tx id ${signedTx.id}")
                    }
                } else if (flipState.headsSold + flipState.tailsSold >= flipState.ticketsToSell ||
                    flipState.headsSold > 0 && flipState.tailsSold > 0 && flipState.endTimeMs < System.currentTimeMillis()
                ) {
                    logger.info("Closing flip round...")

                    nodePeerService.getErgoClient().execute { ctx ->
                        val unsignedTx = flipState.closeRoundAndInitNext(ctx, feeAddress!!)
                        val signedTx = ctx.newProverBuilder().build().sign(unsignedTx)
                        ctx.sendTransaction(signedTx)
                        logger.info("Done, submitted tx id ${signedTx.id}")
                    }
                }
            } catch (t: Throwable) {
                logger.error("Error managing flip", t)
            }

            if (coolDownState == null)
                explorerApiService.getBoxesByAddress(
                    Constants.coolDownAddrString,
                    0,
                    20,
                    false
                ).items?.forEach { coolDownBox ->
                    nodePeerService.getBoxDataById(coolDownBox.boxId)?.let { coolDownOutput ->
                        try {
                            coolDownState = CoolDownOperations(InputBoxImpl(coolDownOutput))
                            logger.info(
                                "Found cooldown box ${coolDownOutput.boxId}, can transition in ${
                                    millisecondsToRelativeTime(coolDownState!!.transitioningTime)
                                }"
                            )
                        } catch (t: Throwable) {
                            logger.error("Error checking cooldown box ${coolDownBox.boxId}", t)
                        }
                    }

                }

            lastManagedBoxId = currentFlipBoxId
            lastManagedMs = System.currentTimeMillis()
        }

        coolDownState?.let { coolDownState ->
            if (coolDownState.transitioningTime < System.currentTimeMillis()) try {
                logger.info("Cooldown transitioning to payout...")
                // get the oracle rate box
                val rateBox =
                    explorerApiService.getBoxesByTokenId(Constants.oracleNftId, 0, 1)[0]
                nodePeerService.getErgoClient().execute { ctx ->
                    val unsignedTx =
                        coolDownState.transitionToPayout(
                            ctx,
                            ctx.dataSource.getBoxById(rateBox.boxId),
                            feeAddress!!,
                            config.getTxFee()
                        )
                    val signedTx = ctx.newProverBuilder().build().sign(unsignedTx)
                    ctx.sendTransaction(signedTx)
                    logger.info("Done, submitted tx id ${signedTx.id}")
                }
            } catch (t: Throwable) {
                logger.error("Error transitioning cooldown box", t)
                this.coolDownState = null
            }
        }
    }

    fun getTicketPurchaseTransaction(
        userAddress: String,
        isHeads: Boolean
    ): ReducedTransaction {
        updateObolFlipState(forced = true)
        val flipState = getCurrentObolFlipState()

        if (flipState.isInitializing)
            throw IllegalStateException("Currently initializing next round, please try again in a few moments.")

        try {
            return nodePeerService.getErgoClient().execute { ctx ->
                val boxOperations = BoxOperations.createForSender(Address.create(userAddress), ctx)
                    .withAmountToSpend(flipState.ticketPrice)
                    .withFeeAmount(flipState.txFee)
                    .withInputBoxesLoader(ExplorerAndPoolUnspentBoxesLoader())

                val unsignedTx = flipState.purchaseTicket(
                    ctx,
                    isHeads,
                    boxOperations.loadTop(),
                    boxOperations.senders.first()
                )

                ctx.newProverBuilder().build().reduce(unsignedTx, 0)
            }
        } catch (e: InputBoxesSelectionException.NotEnoughErgsException) {
            throw IllegalStateException("Only ${formatErgAmount(e.balanceFound)} ERG found on this address.")
        } catch (t: Throwable) {
            logger.error("Error preparing ticket purchase $userAddress", t)
            throw t
        }
    }
}