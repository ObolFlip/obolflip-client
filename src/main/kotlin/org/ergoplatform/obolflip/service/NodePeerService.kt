package org.ergoplatform.obolflip.service

import okhttp3.OkHttpClient
import org.ergoplatform.appkit.ErgoClient
import org.ergoplatform.appkit.RestApiErgoClient
import org.ergoplatform.obolflip.Config
import org.ergoplatform.obolflip.Constants
import org.ergoplatform.restapi.client.ErgoTransaction
import org.ergoplatform.restapi.client.ErgoTransactionOutput
import org.ergoplatform.restapi.client.TransactionsApi
import org.ergoplatform.restapi.client.UtxoApi
import org.springframework.stereotype.Service
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Service
class NodePeerService(
    config: Config,
    private val okHttpClient: OkHttpClient,
) {
    private val url = if (Constants.isMainNet) config.nodeUrl
    else "http://213.239.193.208:9052/"

    fun getErgoClient(): ErgoClient {
        return RestApiErgoClient.createWithHttpClientBuilder(
            url,
            Constants.networkType,
            "",
            if (Constants.isMainNet) RestApiErgoClient.defaultMainnetExplorerUrl else RestApiErgoClient.defaultTestnetExplorerUrl,
            okHttpClient.newBuilder()
        )
    }

    fun getBoxDataById(boxId: String, withPool: Boolean = false): ErgoTransactionOutput? {
        return try {
            val utxoApi = getRetrofitForPeer().create(UtxoApi::class.java)
            val apiCall = if (withPool) utxoApi.getBoxWithPoolById(boxId).execute()
            else utxoApi.getBoxById(boxId).execute()

            apiCall.body()
        } catch (t: Throwable) {
            null
        }
    }

    fun getUnconfirmedTransactions(): List<ErgoTransaction>? {
        return try {
            val txApi = getRetrofitForPeer().create(TransactionsApi::class.java)
            val apiCall = txApi.getUnconfirmedTransactions(500, 0).execute()

            apiCall.body()
        } catch (t: Throwable) {
            null
        }
    }

    private fun getRetrofitForPeer() = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient.newBuilder().connectTimeout(5, TimeUnit.SECONDS).build())
        .build()
}


