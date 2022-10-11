package org.ergoplatform.obolflip

import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.BoxOperations
import org.ergoplatform.ergopay.ErgoPayResponse
import org.ergoplatform.obolflip.handler.ObolFlipBoxOperations
import org.ergoplatform.obolflip.service.NodePeerService
import org.ergoplatform.obolflip.service.ObolFlipService
import org.ergoplatform.obolflip.service.PayoutService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@CrossOrigin
class ErgoPayController(
    private val nodePeerService: NodePeerService,
    private val obolFlipService: ObolFlipService,
    private val payoutService: PayoutService,
) {
    @GetMapping("/ergopay/firstinit/{address}")
    fun initFlip(@PathVariable address: String): ErgoPayResponse {
        val ergoPayResponse = ErgoPayResponse()

        try {
            nodePeerService.getErgoClient().execute { ctx ->
                val boxOperations = BoxOperations.createForSender(Address.create(address), ctx)

                val unsignedTx = ObolFlipBoxOperations.firstInitOrUpdate(boxOperations)

                ergoPayResponse.reducedTx = Base64.getUrlEncoder()
                    .encodeToString(ctx.newProverBuilder().build().reduce(unsignedTx, 0).toBytes())
            }

        } catch (t: Throwable) {
            ergoPayResponse.messageSeverity = ErgoPayResponse.Severity.ERROR
            ergoPayResponse.message = t.message
        }

        return ergoPayResponse
    }

    @GetMapping("/ergopay/purchaseTicket/{tokenType}/{address}")
    fun purchaseTicket(
        @PathVariable address: String,
        @PathVariable tokenType: Int
    ): ErgoPayResponse {
        val ergoPayResponse = ErgoPayResponse()
        val isHead = tokenType == 0

        try {
            ergoPayResponse.reducedTx = Base64.getUrlEncoder().encodeToString(
                obolFlipService.getTicketPurchaseTransaction(address, isHead).toBytes()
            )
            ergoPayResponse.message = "Please confirm to bet on ${if (isHead) "heads" else "tails"}."
            ergoPayResponse.messageSeverity = ErgoPayResponse.Severity.INFORMATION

        } catch (t: Throwable) {
            ergoPayResponse.messageSeverity = ErgoPayResponse.Severity.ERROR
            ergoPayResponse.message = t.message
        }

        return ergoPayResponse
    }

    @GetMapping("/ergopay/redeem/{roundNumber}/{address}")
    fun redeemTicket(
        @PathVariable address: String,
        @PathVariable roundNumber: Int
    ): ErgoPayResponse {
        val ergoPayResponse = ErgoPayResponse()

        try {
            ergoPayResponse.reducedTx = Base64.getUrlEncoder().encodeToString(
                payoutService.redeem(address, roundNumber).toBytes()
            )
            ergoPayResponse.message = "Please confirm redeeming your ticket."
            ergoPayResponse.messageSeverity = ErgoPayResponse.Severity.INFORMATION

        } catch (t: Throwable) {
            ergoPayResponse.messageSeverity = ErgoPayResponse.Severity.ERROR
            ergoPayResponse.message = t.message
        }

        return ergoPayResponse
    }
}