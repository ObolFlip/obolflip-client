package org.ergoplatform.obolflip.handler

import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.obolflip.Constants
import special.collection.Coll

class ObolFlipBoxOperations(
    val currentFlipBox: InputBox,
    private val networkType: NetworkType = NetworkType.MAINNET,
    var txFee: Long = Parameters.MinFee,
) {
    private val flipConfig = currentFlipBox.registers[3].value as Coll<Long>
    val roundNumber = flipConfig.apply(0)
    val isInitializing = flipConfig.apply(1) == Constants.flipStateInit
    val ticketsToSell = flipConfig.apply(2)
    val endTimeMs = flipConfig.apply(3)
    val ticketPrice = flipConfig.apply(4)
    val headsSold = flipConfig.apply(5)
    val tailsSold = flipConfig.apply(6)

    fun purchaseTicket(
        ctx: BlockchainContext,
        buyHeads: Boolean,
        inputBoxes: List<InputBox>, // must satisfy fee and ticket price
        userAddress: Address,
    ): UnsignedTransaction {
        val txB = ctx.newTxBuilder()

        val newFlipBox = txB.outBoxBuilder()
            .contract(ErgoTreeContract(currentFlipBox.ergoTree, networkType))
            .value(currentFlipBox.value + ticketPrice)
            .tokens(
                currentFlipBox.tokens[0],
                if (buyHeads) ErgoToken(
                    currentFlipBox.tokens[1].id,
                    currentFlipBox.tokens[1].value - 1
                ) else currentFlipBox.tokens[1],
                if (buyHeads) currentFlipBox.tokens[2]
                else ErgoToken(
                    currentFlipBox.tokens[2].id,
                    currentFlipBox.tokens[2].value - 1
                )
            )
            .registers(
                currentFlipBox.registers[0],
                currentFlipBox.registers[1],
                currentFlipBox.registers[2],
                ErgoValue.of(
                    arrayOf(
                        flipConfig.apply(0),
                        flipConfig.apply(1),
                        flipConfig.apply(2),
                        flipConfig.apply(3),
                        flipConfig.apply(4),
                        flipConfig.apply(5) + if (buyHeads) 1 else 0,
                        flipConfig.apply(6) + if (buyHeads) 0 else 1,
                    ), ErgoType.longType()
                )
            )
            .build()

        val receiptBox = txB.outBoxBuilder()
            .value(Parameters.MinChangeValue)
            .tokens(ErgoToken(currentFlipBox.tokens[if (buyHeads) 1 else 2].id, 1))
            .contract(userAddress.toErgoContract())
            .build()

        return txB
            .boxesToSpend(listOf(currentFlipBox) + inputBoxes)
            .sendChangeTo(userAddress.ergoAddress)
            .fee(txFee)
            .outputs(newFlipBox, receiptBox)
            .build()
    }

    fun finalizeInit(
        ctx: BlockchainContext,
        feeAddress: Address,
    ): UnsignedTransaction {
        val tokenToMint = Eip4Token(
            currentFlipBox.id.toString(),
            ticketsToSell + 1,
            Constants.tailTokenNamePrefix + roundNumber.toString(),
            "ObolFlip, the first coin flip on ergo",
            0
        )

        val txB = ctx.newTxBuilder()

        val newFlipBox = txB.outBoxBuilder()
            .contract(ErgoTreeContract(currentFlipBox.ergoTree, networkType))
            .value(Parameters.MinChangeValue)
            .tokens(currentFlipBox.tokens[0], currentFlipBox.tokens[1], tokenToMint)
            .registers(
                tokenToMint.mintingBoxR4,
                tokenToMint.mintingBoxR5,
                tokenToMint.mintingBoxR6,
                ErgoValue.of(
                    arrayOf(
                        flipConfig.apply(0),
                        Constants.flipStateActive,
                        flipConfig.apply(2),
                        flipConfig.apply(3),
                        flipConfig.apply(4),
                        0L,
                        0L,
                    ), ErgoType.longType()
                )
            )
            .build()

        val softwareFeeBox = txB.outBoxBuilder()
            .contract(Constants.softwareLicenseFee.toErgoContract())
            .value((currentFlipBox.value - txFee - newFlipBox.value) * 30 / 100)
            .build()

        return txB.boxesToSpend(listOf(currentFlipBox))
            .sendChangeTo(feeAddress.ergoAddress)
            .outputs(newFlipBox, softwareFeeBox)
            .fee(txFee)
            .build()
    }

    fun closeRoundAndInitNext(
        ctx: BlockchainContext,
        feeAddress: Address
    ): UnsignedTransaction {
        val txB = ctx.newTxBuilder()
        val newRoundNumber = roundNumber + 1

        val tokenToMint = Eip4Token(
            currentFlipBox.id.toString(),
            Constants.ticketsToSell + 1,
            Constants.headTokenNamePrefix + newRoundNumber.toString(),
            "ObolFlip, the first coin flip on ergo",
            0
        )
        val newFlipBox = txB.outBoxBuilder()
            .contract(ErgoTreeContract(currentFlipBox.ergoTree, networkType))
            .value(currentFlipBox.value / 100)
            .tokens(currentFlipBox.tokens[0], tokenToMint)
            .registers(
                tokenToMint.mintingBoxR4,
                tokenToMint.mintingBoxR5,
                tokenToMint.mintingBoxR6,
                ErgoValue.of(
                    arrayOf(
                        newRoundNumber,
                        Constants.flipStateInit,
                        Constants.ticketsToSell,
                        // we add 10 minutes here to be sure
                        System.currentTimeMillis() + (Constants.maxRoundTimeHours * 60 + 10) * 1000L * 60,
                        Constants.ticketPrice,
                        0L,
                        0L,
                    ), ErgoType.longType()
                )
            )
            .build()

        val coolDownBox = txB.outBoxBuilder()
            .contract(Constants.coolDownAddress.toErgoContract())
            .value(currentFlipBox.value * 98 / 100)
            .tokens(currentFlipBox.tokens[1], currentFlipBox.tokens[2])
            .registers(
                ErgoValue.of(roundNumber),
                ErgoValue.of(endTimeMs + Constants.coolDownMinutes * 60 * 1000),
                ErgoValue.of(
                    arrayOf(headsSold, tailsSold),
                    ErgoType.longType()
                ),
            )
            .build()

        val softwareFeeBox = txB.outBoxBuilder()
            .contract(Constants.softwareLicenseFee.toErgoContract())
            .value((currentFlipBox.value - txFee - coolDownBox.value - newFlipBox.value) * 30 / 100)
            .build()

        return txB.boxesToSpend(listOf(currentFlipBox))
            .sendChangeTo(feeAddress.ergoAddress)
            .outputs(newFlipBox, coolDownBox, softwareFeeBox)
            .fee(txFee)
            .build()
    }

    companion object {

        /**
         * this is the very first init, normally not used
         */
        fun firstInitOrUpdate(boxOperations: BoxOperations): UnsignedTransaction {
            val markerNft = ErgoToken(Constants.markerNftId, 1)
            val initValue = Parameters.OneErg

            boxOperations.withAmountToSpend(initValue).withTokensToSpend(listOf(markerNft))

            return boxOperations.buildTxWithDefaultInputs { txB ->

                val tokenToMint = Eip4Token(
                    txB.inputBoxes[0].id.toString(),
                    Constants.ticketsToSell + 1,
                    Constants.headTokenNamePrefix + 1.toString(),
                    "ObolFlip, the first coin flip on ergo",
                    0
                )

                val output = txB.outBoxBuilder()
                    .contract(Constants.obolFlipAddress.toErgoContract())
                    .tokens(
                        markerNft,
                        tokenToMint
                    )
                    .value(initValue)
                    .registers(
                        tokenToMint.mintingBoxR4,
                        tokenToMint.mintingBoxR5,
                        tokenToMint.mintingBoxR6,
                        ErgoValue.of(
                            arrayOf(
                                1L,
                                Constants.flipStateInit,
                                Constants.ticketsToSell,
                                System.currentTimeMillis() + ((Constants.maxRoundTimeHours * 60 + 10) * 1000L * 60), // max time
                                Parameters.OneErg, // ticket price
                                0L, // heads sold
                                0L, // tails sold
                            ), ErgoType.longType()
                        )
                    )
                    .build()

                txB.outputs(output)

                return@buildTxWithDefaultInputs txB
            }
        }
    }
}