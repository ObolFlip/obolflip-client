package org.ergoplatform.obolflip

import org.ergoplatform.appkit.*
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import scorex.crypto.hash.Blake2b256

class ObolFlipContracts {
    private val networkType = Constants.networkType
    val updateNftId =
        if (Constants.isMainNet) "284897f396fcd63061b4b1d7470ccbb4ecc94872a8cc5f5b8512396e0265825a"
        else "06eeb994c70d7a87e1af417dab0113ef48aa6a676be9c0baa075814e632c2cb8"

    lateinit var payoutContract: ErgoContract
    lateinit var cooldownContract: ErgoContract
    lateinit var obolFlipContract: ErgoContract

    init {

        val ergoClient = ColdErgoClient(networkType, 0)
        ergoClient.execute { ctx ->

            payoutContract =
                ctx.compileContract(ConstantsBuilder.empty(), getScript("payout.es"))

            cooldownContract =
                ctx.compileContract(
                    ConstantsBuilder.create()
                        .item("randomBoxToken", ErgoId.create(Constants.oracleNftId).bytes)
                        .item("payoutBoxHash", Blake2b256.hash(payoutContract.ergoTree.bytes()))
                        .build(),
                    getScript("cooldown.es")
                )

            obolFlipContract =
                ctx.compileContract(
                    ConstantsBuilder.create()
                        .item("configTicketsToSell", Constants.ticketsToSell)
                        .item("configMaxTime", 1000L * 60 * 60 * (Constants.maxRoundTimeHours))
                        .item("configTicketPrice", Constants.ticketPrice)
                        .item("configHeadPrefix", Constants.headTokenNamePrefix.encodeToByteArray())
                        .item("configTailPrefix", Constants.tailTokenNamePrefix.encodeToByteArray())
                        .item("configCooldown", 1000L * 60 * Constants.coolDownMinutes)
                        .item("cooldownBoxHash", Blake2b256.hash(cooldownContract.ergoTree.bytes()))
                        .item("updateNFT", ErgoId.create(updateNftId).bytes)
                        .build(),
                    getScript("obolflip.es")
                )
        }
    }

    @Test
    fun createAddresses() {
        println("Mainnet: ${Constants.isMainNet}")
        println("Payout contract: ${payoutContract.toAddress()}")
        assert(payoutContract.toAddress() == Constants.payoutAddress)
        println("Cooldown contract: ${cooldownContract.toAddress()}")
        assert(cooldownContract.toAddress() == Constants.coolDownAddress)
        println("Flip contract: ${obolFlipContract.toAddress()}")
        assert(obolFlipContract.toAddress() == Constants.obolFlipAddress)
    }

    private fun getScript(filename: String): String {
        val contract = ClassPathResource(filename).inputStream.bufferedReader().use {
            it.readText()
        }
        return contract
    }
}