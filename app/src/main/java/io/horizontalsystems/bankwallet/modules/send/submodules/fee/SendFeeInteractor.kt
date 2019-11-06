package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.entities.FeeRateInfo
import java.math.BigDecimal

class SendFeeInteractor(private val rateManager: IRateManager,
                        private val feeRateProvider: IFeeRateProvider?,
                        private val currencyManager: ICurrencyManager) : SendFeeModule.IInteractor {

    override fun getRate(coinCode: String): BigDecimal? {
        return rateManager.getLatestRate(coinCode, currencyManager.baseCurrency.code)
    }

    override fun getFeeRates(): List<FeeRateInfo>? {
        return feeRateProvider?.feeRates()
    }

}
