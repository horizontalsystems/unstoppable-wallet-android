package cash.p.terminal.tangem.domain.task

import cash.p.terminal.tangem.common.TwinsHelper
import cash.p.terminal.tangem.domain.model.ProductType
import cash.p.terminal.tangem.domain.model.ScanResponse
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.extensions.toHexString
import com.tangem.operations.issuerAndUserData.ReadIssuerDataCommand

internal class ScanTwinProcessor : ProductCommandProcessor<ScanResponse> {
    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        ReadIssuerDataCommand().run(session) { readDataResult ->
            when (readDataResult) {
                is CompletionResult.Success -> {
                    val publicKey = card.wallets.firstOrNull()?.publicKey
                    if (publicKey == null) {
                        val response = ScanResponse(
                            card = card,
                            productType = ProductType.Twins,
                            walletData = null,
                        )
                        callback(CompletionResult.Success(response))
                        return@run
                    }

                    val verified =
                        TwinsHelper.verifyTwinPublicKey(readDataResult.data.issuerData, publicKey)
                    val response = if (verified) {
                        val twinPublicKey = readDataResult.data.issuerData.sliceArray(0 until 65)
                        val walletData = session.environment.walletData
                        ScanResponse(
                            card = card,
                            productType = ProductType.Twins,
                            walletData = walletData,
                            secondTwinPublicKey = twinPublicKey.toHexString(),
                        )
                    } else {
                        ScanResponse(
                            card = card,
                            productType = ProductType.Twins,
                            walletData = null,
                        )
                    }
                    callback(CompletionResult.Success(response))
                }

                is CompletionResult.Failure -> {
                    callback(CompletionResult.Success(ScanResponse(card, ProductType.Twins, null)))
                }
            }
        }
    }
}
