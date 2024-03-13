package io.horizontalsystems.bankwallet.modules.swapxxx.providers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

abstract class EvmSwapProvider : ISwapXxxProvider {
    protected suspend fun getAllowance(token: Token, spenderAddress: Address): BigDecimal? {
        if (token.type !is TokenType.Eip20) return null

        val eip20Adapter = App.adapterManager.getAdapterForToken(token)

        if (eip20Adapter !is Eip20Adapter) return null

        return eip20Adapter.allowance(spenderAddress, DefaultBlockParameter.Latest).await()
    }
}
