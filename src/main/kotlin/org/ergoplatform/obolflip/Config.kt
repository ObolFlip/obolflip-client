package org.ergoplatform.obolflip

import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.Parameters
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("obolflip")
class Config {
    lateinit var nodeUrl: String
    var publicurl: String = ""

    var feeAddress: String = ""
    var manageFlip: Boolean = false
    var manageTxFeeFactor: Double = 1.0

    fun getFeeAddress(): Address? =
        if (feeAddress.isNotBlank()) {
            try {
                Address.create(feeAddress)
            } catch (t: Throwable) {
                null
            }
        } else null

    fun getTxFee(): Long = (Parameters.MinFee * manageTxFeeFactor).toLong()

    val ergopayPrefix by lazy {
        publicurl.replace("http://", "ergopay://").replace("https://", "ergopay://")
            .trimEnd('/') + "/ergopay"
    }
}