package io.horizontalsystems.bankwallet.modules.walletconnect.stellar

import com.google.gson.GsonBuilder
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.walletconnect.request.AbstractWCAction
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCActionState
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.stellarkit.StellarKit
import kotlinx.coroutines.CoroutineScope

class WCActionStellarSignXdr(
    private val paramsJsonStr: String,
    private val peerName: String,
    private val stellarKit: StellarKit,
) : AbstractWCAction() {

    private val gson = GsonBuilder().create()
    private val params = gson.fromJson(paramsJsonStr, Params::class.java)
    private val xdr = params.xdr

    override fun getTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.WalletConnect_SignMessageRequest_Title)
    }

    override fun getApproveButtonTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.Button_Sign)
    }

    override suspend fun performAction(): String {
        return gson.toJson(mapOf("signedXDR" to stellarKit.signTransaction(params.xdr)))
    }

    override fun start(coroutineScope: CoroutineScope) = Unit

    override fun createState(): WCActionState {
        val transaction = stellarKit.getTransaction(xdr)

        return WCActionState(
            runnable = true,
            items = WCStellarHelper.getTransactionViewItems(transaction, xdr, peerName)
        )
    }

    data class Params(val xdr: String)
}
