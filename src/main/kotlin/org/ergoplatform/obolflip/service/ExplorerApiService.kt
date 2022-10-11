package org.ergoplatform.obolflip.service

import okhttp3.OkHttpClient
import org.ergoplatform.appkit.RestApiErgoClient
import org.ergoplatform.explorer.client.DefaultApi
import org.ergoplatform.explorer.client.model.ItemsA
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.obolflip.Constants
import org.springframework.stereotype.Service
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

@Service
class ExplorerApiService(private val okHttpClient: OkHttpClient) {
    val timeout = 30L // 30 seconds since Explorer can be slooooow

    private val api by lazy {
        buildExplorerApi(
            if (Constants.isMainNet) RestApiErgoClient.defaultMainnetExplorerUrl
            else RestApiErgoClient.defaultTestnetExplorerUrl
        )
    }

    private fun buildExplorerApi(url: String) = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            okHttpClient.newBuilder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS).build()
        )
        .build()
        .create(DefaultApi::class.java)

    fun getBoxesByTokenId(tokenId: String, offset: Int, limit: Int): List<OutputInfo> =
        wrapCall { api.getApiV1BoxesUnspentBytokenidP1(tokenId, offset, limit) }.items

    private fun <T> wrapCall(call: () -> Call<T>): T {
        val explorerCall = call().execute()

        if (!explorerCall.isSuccessful)
            throw IOException("Error calling Explorer: ${explorerCall.errorBody()}")

        return explorerCall.body()!!
    }

    fun getBoxesByAddress(
        address: String,
        offset: Int,
        limit: Int,
        ascending: Boolean
    ): ItemsA =
        wrapCall {
            api.getApiV1BoxesUnspentByaddressP1(
                address,
                offset,
                limit,
                if (ascending) "asc" else "desc"
            )
        }

    fun getBoxInformation(boxId: String) =
        wrapCall {
            api.getApiV1BoxesP1(boxId)
        }

}
