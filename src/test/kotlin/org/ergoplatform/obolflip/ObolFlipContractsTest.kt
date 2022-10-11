package org.ergoplatform.obolflip

import okhttp3.OkHttpClient
import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.impl.BlockchainContextImpl
import org.ergoplatform.obolflip.handler.CoolDownOperations
import org.ergoplatform.obolflip.handler.ObolFlipBoxOperations
import org.ergoplatform.obolflip.handler.PayoutBox
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ObolFlipContractsTest {

    @Autowired
    var config: Config? = null

    private val contracts = ObolFlipContracts()
    private val ergoClient by lazy {
        RestApiErgoClient.createWithHttpClientBuilder(
            if (Constants.isMainNet) config!!.nodeUrl
            else "http://213.239.193.208:9052/",
            Constants.networkType,
            "",
            if (Constants.isMainNet) RestApiErgoClient.defaultMainnetExplorerUrl else RestApiErgoClient.defaultTestnetExplorerUrl,
            OkHttpClient.Builder()
        )
    }

    private val headsTokenId = "a115b11c9b9ccefe19f176bcb9a78e06cbdbf7565daafa103bb7f543d06a5704"
    private val tailsTokenId = "fcb0b7f408a4df40d20f6746ea6228288c57c19f7143cd4aa371cfdced65df86"
    private val roundNumber = 4711L
    private val timeStamp = System.currentTimeMillis()
    private val mockTxId = "06d52cd0c85cf0c7a6ef3e6ec8b91ffeca42b9a554ebff4c510ceb4ce031fb5a"
    private val mockTxId2 = "a80d97e53b4e249ad2acc9e31c2f742aad5068167f3ed419353ee52a6725d540"


    private val userAddressMnemonic = SecretString.create(Mnemonic.generateEnglishMnemonic())
    private val userAddress = Address.createEip3Address(
        0,
        Constants.networkType,
        userAddressMnemonic,
        SecretString.empty()
    )

    @Test
    fun testNextInit() {
        ergoClient.execute { ctx ->
            val txB = ctx.newTxBuilder()

            val inputObolFlipBox = getActiveObolFlip(txB, isDone = true)

            val unsignedTx = ObolFlipBoxOperations(
                inputObolFlipBox,
                Constants.networkType
            ).closeRoundAndInitNext(ctx, userAddress)

            ctx.newProverBuilder().withMnemonic(userAddressMnemonic, SecretString.empty())
                .withEip3Secret(0).build().sign(unsignedTx)
        }
    }

    @Test
    fun testInitDone() {
        ergoClient.execute { ctx ->
            val txB = ctx.newTxBuilder()
            val initFlipBox = txB.outBoxBuilder()
                .contract(contracts.obolFlipContract)
                .value(Parameters.OneErg)
                .tokens(
                    ErgoToken(Constants.markerNftId, 1),
                    ErgoToken(headsTokenId, Constants.ticketsToSell + 1),
                )
                .registers(
                    ErgoValue.of("NFT name".encodeToByteArray()),
                    ErgoValue.of("NFT desc".encodeToByteArray()),
                    ErgoValue.of("0".encodeToByteArray()),
                    ErgoValue.of(
                        arrayOf(
                            roundNumber,
                            Constants.flipStateInit,
                            Constants.ticketsToSell,
                            timeStamp, // max time
                            Parameters.OneErg, // ticket price
                            0L, // heads sold
                            0L, // tails sold
                        ), ErgoType.longType()
                    )
                )
                .build()
                .convertToInputWith(mockTxId, 0)

            val unsignedTx =
                ObolFlipBoxOperations(initFlipBox, Constants.networkType).finalizeInit(
                    ctx, userAddress
                )

            ctx.newProverBuilder().withMnemonic(userAddressMnemonic, SecretString.empty())
                .withEip3Secret(0).build().sign(unsignedTx)
        }
    }

    @Test
    fun testTicketPurchase() {
        ergoClient.execute { ctx ->
            val txB = ctx.newTxBuilder()

            val inputObolFlipBox = getActiveObolFlip(txB)

            val userPaymentBox = txB.outBoxBuilder()
                .value(Parameters.OneErg * 10)
                .contract(userAddress.toErgoContract())
                .build()
                .convertToInputWith(mockTxId, 0)

            for (i in 0..1) {
                val buyHeads = i == 0

                val obolFlipHandler =
                    ObolFlipBoxOperations(inputObolFlipBox, Constants.networkType)

                val unsignedTx = obolFlipHandler.purchaseTicket(
                    ctx,
                    buyHeads,
                    listOf(userPaymentBox),
                    userAddress
                )


                ctx.newProverBuilder().withMnemonic(userAddressMnemonic, SecretString.empty())
                    .withEip3Secret(0).build().sign(unsignedTx)
            }
        }
    }

    private fun getActiveObolFlip(txB: UnsignedTransactionBuilder, isDone: Boolean = false) =
        txB.outBoxBuilder()
            .value(50 * Parameters.OneErg)
            .contract(contracts.obolFlipContract)
            .tokens(
                ErgoToken(Constants.markerNftId, 1),
                ErgoToken(headsTokenId, 50),
                ErgoToken(tailsTokenId, 45)
            )
            .registers(
                ErgoValue.of("NFT name".encodeToByteArray()),
                ErgoValue.of("NFT desc".encodeToByteArray()),
                ErgoValue.of("0".encodeToByteArray()),
                ErgoValue.of(
                    arrayOf(
                        roundNumber,
                        Constants.flipStateActive,
                        100 * Parameters.OneErg, // max amount
                        System.currentTimeMillis() + (if (isDone) -1 else 1) * 1000L * 60 * 10, // max time
                        Parameters.OneErg, // ticket price
                        50L, // heads sold
                        55L, // tails sold
                    ), ErgoType.longType()
                )
            )
            .build()
            .convertToInputWith(mockTxId, 0)

    @Test
    fun testUpdateContract() {
        ergoClient.execute { ctx ->
            val txB = ctx.newTxBuilder()

            val inputObolFlipBox = getActiveObolFlip(txB)

            val updateNftBox = txB.outBoxBuilder()
                .value(Parameters.OneErg)
                .contract(userAddress.toErgoContract())
                .tokens(ErgoToken(contracts.updateNftId, 1))
                .build()
                .convertToInputWith(mockTxId, 0)

            val receiptBox = txB.outBoxBuilder()
                .value(inputObolFlipBox.value - Parameters.MinFee)
                .tokens(*inputObolFlipBox.tokens.toTypedArray())
                .contract(userAddress.toErgoContract())
                .registers(*inputObolFlipBox.registers.toTypedArray())
                .build()

            val unsignedTx = ctx.newTxBuilder()
                .boxesToSpend(listOf(updateNftBox, inputObolFlipBox))
                .sendChangeTo(userAddress.ergoAddress)
                .outputs(receiptBox)
                .build()

            ctx.newProverBuilder().withMnemonic(userAddressMnemonic, SecretString.empty())
                .withEip3Secret(0).build().sign(unsignedTx)
        }
    }

    @Test
    fun testCooldownTransition() {
        ergoClient.execute { ctx ->
            val txB = ctx.newTxBuilder()

            val inputCooldownBox = txB.outBoxBuilder()
                .value(100 * Parameters.OneErg)
                .contract(contracts.cooldownContract)
                .tokens(ErgoToken(headsTokenId, 1), ErgoToken(tailsTokenId, 1))
                .registers(
                    ErgoValue.of(roundNumber),
                    ErgoValue.of((ctx as BlockchainContextImpl).headers.first().timestamp - 1000),
                    ErgoValue.of(arrayOf(100L, 100L), ErgoType.longType()),
                )
                .build()
                .convertToInputWith(mockTxId, 0)

            val dataBox = txB.outBoxBuilder()
                .value(Parameters.MinChangeValue)
                .contract(ctx.compileContract(ConstantsBuilder.empty(), "{ sigmaProp(false) }"))
                .tokens(ErgoToken(Constants.oracleNftId, 1))
                .build()
            val dataInput1 = dataBox.convertToInputWith(mockTxId, 0)
            val dataInput2 = dataBox.convertToInputWith(mockTxId2, 1)

            for (i in 0..1) {
                val dataInput = if (i == 0) dataInput2 else dataInput1

                val unsignedTx = CoolDownOperations(inputCooldownBox).transitionToPayout(
                    ctx,
                    dataInput,
                    userAddress,
                    Parameters.MinFee
                )

                ctx.newProverBuilder().withMnemonic(userAddressMnemonic, SecretString.empty())
                    .withEip3Secret(0).build().sign(unsignedTx)
            }
        }

    }

    @Test
    fun testPayout() {
        ergoClient.execute { ctx ->
            val txB = ctx.newTxBuilder()

            val inputPayoutBox = txB.outBoxBuilder()
                .value(100 * Parameters.OneErg)
                .contract(contracts.payoutContract)
                .tokens(ErgoToken(headsTokenId, 1))
                .registers(
                    ErgoValue.of(roundNumber),
                    ErgoValue.of(100L),
                    ErgoValue.of(100 * Parameters.OneErg),
                    ErgoValue.of(0L),
                )
                .build()
                .convertToInputWith(mockTxId, 0)

            val userTokenBox = txB.outBoxBuilder()
                .value(Parameters.MinChangeValue)
                .contract(userAddress.toErgoContract())
                .tokens(ErgoToken(headsTokenId, 1))
                .build()
                .convertToInputWith(mockTxId, 0)

            val unsignedTx = PayoutBox(inputPayoutBox).redeemTicket(
                ctx, userAddress, userTokenBox,
                listOf(Pair(70, userAddress), Pair(30, userAddress))
            )

            ctx.newProverBuilder().withMnemonic(userAddressMnemonic, SecretString.empty())
                .withEip3Secret(0).build().sign(unsignedTx)
        }
    }
}