package io.horizontalsystems.walletkit.modules.walletconnect.solana

import com.google.gson.GsonBuilder
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ValueType
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.walletkit.modules.walletconnect.request.AbstractWCAction
import io.horizontalsystems.walletkit.modules.walletconnect.request.WCActionState
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.solanakit.Signer
import kotlinx.coroutines.CoroutineScope

// Handles `solana_signMessage`. Params: { message: <base58>, pubkey: <base58> }.
// Response: { signature: <base58> }.
class WCActionSolanaSignMessage(
    private val paramsJsonStr: String,
    private val signer: Signer,
) : AbstractWCAction() {

    private val gson = GsonBuilder().create()
    private val params = gson.fromJson(paramsJsonStr, Params::class.java)

    private val messageBytes = WCSolanaHelper.base58Decode(params.message)

    override fun getTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.WalletConnect_SignMessageRequest_Title)
    }

    override fun getApproveButtonTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.Button_Sign)
    }

    override suspend fun performAction(): String {
        val signature = signer.signMessage(messageBytes)
        return gson.toJson(mapOf("signature" to WCSolanaHelper.base58Encode(signature)))
    }

    override fun start(coroutineScope: CoroutineScope) = Unit

    override fun createState(): WCActionState {
        val messageText = String(messageBytes)

        var sectionViewItems = listOf(
            SectionViewItem(
                listOf(ViewItem.Input(Translator.getString(R.string.WalletConnect_SignMessageRequest_Title), messageText))
            )
        )

        App.accountManager.activeAccount?.name?.let { walletName ->
            sectionViewItems += SectionViewItem(
                listOf(
                    ViewItem.Value(
                        Translator.getString(R.string.Wallet_Title),
                        walletName,
                        ValueType.Regular
                    )
                )
            )
        }

        return WCActionState(
            runnable = true,
            items = sectionViewItems
        )
    }

    data class Params(val message: String, val pubkey: String?)
}
