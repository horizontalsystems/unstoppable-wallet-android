package io.horizontalsystems.walletkit.core.providers

import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

/**
 * App/flavor-specific configuration seam.
 *
 * The concrete implementation ([AppConfigProvider]) lives in :app because it reads
 * BuildConfig values that are defined per product-flavor and build-type.
 * :core code (and the App composition root) depend only on this interface, so :core
 * stays reusable by another app that supplies its own configuration.
 */
interface IAppConfigProvider {
    val appId: String?
    val appVersion: String
    val appBuild: Int
    val companyWebPageLink: String
    val appWebPageLink: String
    val analyticsLink: String
    val appGithubLink: String
    val appTwitterLink: String
    val appTelegramLink: String
    val reportEmail: String
    val releaseNotesUrl: String
    val mempoolSpaceUrl: String
    val blockCypherUrl: String
    val walletConnectUrl: String
    val walletConnectProjectId: String
    val walletConnectAppMetaDataName: String
    val walletConnectAppMetaDataUrl: String
    val walletConnectAppMetaDataIcon: String
    val simplexSupportChat: String
    val nymVpnLink: String
    val telegramSupportChat: String
    val blocksDecodedEthereumRpc: String
    val twitterBearerToken: String
    val etherscanApiKey: List<String>
    val bscscanApiKey: List<String>
    val otherScanApiKey: List<String>
    val guidesUrl: String
    val eduUrl: String
    val faqUrl: String
    val coinsJsonUrl: String
    val providerCoinsJsonUrl: String
    val marketApiBaseUrl: String
    val marketApiKey: String
    val openSeaApiKey: String
    val trongridApiKeys: List<String>
    val udnApiKey: String
    val oneInchApiKey: String
    val thorchainApiKey: String
    val appLinksHost: String
    val fiatDecimal: Int
    val feeRateAdjustForCurrencies: List<String>
    val currencies: List<Currency>
    val donateAddresses: Map<BlockchainType, String>
    val spamCoinValueLimits: Map<String, BigDecimal>
    val chainalysisBaseUrl: String
    val chainalysisApiKey: String
    val hashDitBaseUrl: String
    val hashDitApiKey: String
    val uswapApiBaseUrl: String
    val uswapApiKey: String
    val oneInchPartnerFeeAddress: String
    val swapFeeBps: Int
    val fdroidBuild: Boolean

    /**
     * Whether a token that shows up in the account's history (a received transfer, a swap
     * output, a discovered token account) gets a wallet enabled for it automatically. An app
     * that works with a fixed token set turns this off; its wallets then come only from the
     * explicit enable paths.
     */
    val autoEnableTokensOnReceive: Boolean
        get() = true
}
