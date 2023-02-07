package cash.p.terminal.core.providers

import com.google.gson.annotations.SerializedName
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.APIClient
import retrofit2.http.GET

class EvmLabelProvider {
    private val apiURL = App.appConfigProvider.marketApiBaseUrl + "/v1/"
    private val apiService: HsLabelApi = APIClient.retrofit(apiURL, 60).create(HsLabelApi::class.java)

    suspend fun updatesStatus(): UpdatesStatus = apiService.updatesStatus()

    suspend fun evmMethodLabels(): List<EvmMethodLabel> = apiService.evmMethodLabels()

    suspend fun evmAddressLabels(): List<EvmAddressLabel> = apiService.evmAddressLabels()

    data class UpdatesStatus(
        @SerializedName("address_labels")
        val addressLabels: Long,
        @SerializedName("evm_method_labels")
        val evmMethodLabels: Long
    )

    data class EvmMethodLabel(
        @SerializedName("method_id")
        val methodId: String,
        val label: String
    )

    data class EvmAddressLabel(
        val address: String,
        val label: String
    )

    private interface HsLabelApi {
        @GET("status/updates")
        suspend fun updatesStatus(): UpdatesStatus

        @GET("evm-method-labels")
        suspend fun evmMethodLabels(): List<EvmMethodLabel>

        @GET("addresses/labels")
        suspend fun evmAddressLabels(): List<EvmAddressLabel>
    }

}
