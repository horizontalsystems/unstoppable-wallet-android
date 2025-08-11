package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.monerokit.MoneroKit
import android.util.Log

class MoneroAdapter(
    private val kit: MoneroKit
): IAdapter {

    override fun start() {
        Log.e("AAA", "moneroAdapter: start()")
       kit.start()
    }

    override fun stop() {
        Log.e("AAA", "moneroAdapter: stop()")
        kit.stop()
    }

    override fun refresh() {
        Log.e("AAA", "moneroAdapter: refresh()")
//        TODO("not implemented")
    }

    override val debugInfo: String
        get() = TODO("Not yet implemented")


    companion object {
        fun create(context: Context, wallet: Wallet): MoneroAdapter {
            val words = when(val accountType = wallet.account.type) {
                is AccountType.Mnemonic ->  accountType.words
                else -> throw IllegalStateException("Unsupported account type: ${accountType.javaClass.simpleName}")
            }

            val kit = MoneroKit.getInstance(
                context,
                words,
                "",
                wallet.account.id
            )

            return MoneroAdapter(
                kit
            )

        }
    }
}