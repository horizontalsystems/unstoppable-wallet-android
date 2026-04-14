package com.quantum.wallet.bankwallet.modules.walletconnect.stellar

import com.google.gson.GsonBuilder
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.modules.sendevmtransaction.SectionViewItem
import com.quantum.wallet.bankwallet.modules.sendevmtransaction.ValueType
import com.quantum.wallet.bankwallet.modules.sendevmtransaction.ViewItem
import com.quantum.wallet.bankwallet.modules.walletconnect.request.AbstractWCAction
import com.quantum.wallet.bankwallet.modules.walletconnect.request.WCActionState
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
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
        var sectionViewItems = WCStellarHelper.getTransactionViewItems(transaction, xdr)
        App.accountManager.activeAccount?.name?.let { walletName ->
            sectionViewItems += SectionViewItem(
                listOf(ViewItem.Value(
                    Translator.getString(R.string.Wallet_Title),
                    walletName,
                    ValueType.Regular
                ))
            )
        }

        return WCActionState(
            runnable = true,
            items = WCStellarHelper.getTransactionViewItems(transaction, xdr) + sectionViewItems
        )
    }

    data class Params(val xdr: String)
}
