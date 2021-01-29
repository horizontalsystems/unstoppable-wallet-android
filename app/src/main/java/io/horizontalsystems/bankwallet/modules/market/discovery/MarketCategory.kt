package io.horizontalsystems.bankwallet.modules.market.discovery

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.R

sealed class MarketCategory(
        val id: String = "",
        @StringRes val titleResId: Int,
        @DrawableRes val iconResId: Int,
        @StringRes val descriptionResId: Int
) {
    object Rated : MarketCategory("Rated", R.string.Market_Category_Rated, R.drawable.ic_chart, R.string.Market_Category_Rated_Description)
    object Blockchains : MarketCategory("blockchain", R.string.Market_Category_Blockchains, R.drawable.ic_blocks, R.string.Market_Category_Blockchains_Description)
    object Privacy : MarketCategory("privacy", R.string.Market_Category_Privacy, R.drawable.ic_shield, R.string.Market_Category_Privacy_Description)
    object Scaling : MarketCategory("scaling", R.string.Market_Category_Scaling, R.drawable.ic_scale, R.string.Market_Category_Scaling_Description)
    object Infrastructure : MarketCategory("infrastructure", R.string.Market_Category_Infrastructure, R.drawable.ic_settings_2, R.string.Market_Category_Infrastructure_Description)
    object RiskManagement : MarketCategory("risk_management_and_hedging", R.string.Market_Category_RiskManagement, R.drawable.ic_clipboard, R.string.Market_Category_RiskManagement_Description)
    object Oracles : MarketCategory("oracles", R.string.Market_Category_Oracles, R.drawable.ic_eye, R.string.Market_Category_Oracles_Description)
    object PredictionMarkets : MarketCategory("prediction_markets", R.string.Market_Category_PredictionMarkets, R.drawable.ic_markets, R.string.Market_Category_PredictionMarkets_Description)
    object DefiAggregators : MarketCategory("defi_aggregators", R.string.Market_Category_DefiAggregators, R.drawable.ic_portfolio, R.string.Market_Category_DefiAggregators_Description)
    object Dexes : MarketCategory("dexes", R.string.Market_Category_Dexes, R.drawable.ic_swap_2, R.string.Market_Category_Dexes_Description)
    object Synthetics : MarketCategory("synthetics", R.string.Market_Category_Synthetics, R.drawable.ic_flask, R.string.Market_Category_Synthetics_Description)
    object Metals : MarketCategory("metals", R.string.Market_Category_Metals, R.drawable.ic_metals, R.string.Market_Category_Metals_Description)
    object Lending : MarketCategory("lending", R.string.Market_Category_Lending, R.drawable.ic_swap_approval_2, R.string.Market_Category_Lending_Description)
    object GamingAndVr : MarketCategory("gaming_and_vr", R.string.Market_Category_GamingAndVr, R.drawable.ic_game, R.string.Market_Category_GamingAndVr_Description)
    object FundRaising : MarketCategory("fundraising", R.string.Market_Category_Fundraising, R.drawable.ic_download, R.string.Market_Category_Fundraising_Description)
    object InternetOfThings : MarketCategory("iot", R.string.Market_Category_InternetOfThings, R.drawable.ic_globe, R.string.Market_Category_InternetOfThings_Description)
    object B2B : MarketCategory("b2b", R.string.Market_Category_B2B, R.drawable.ic_swap, R.string.Market_Category_B2B_Description)
    object NFT : MarketCategory("nft", R.string.Market_Category_NFT, R.drawable.ic_user, R.string.Market_Category_NFT_Description)
    object Wallets : MarketCategory("wallets", R.string.Market_Category_Wallets, R.drawable.ic_wallet, R.string.Market_Category_Wallets_Description)
    object Staking : MarketCategory("staking", R.string.Market_Category_Staking, R.drawable.ic_plus_circled, R.string.Market_Category_Staking_Description)
    object FiatStablecoins : MarketCategory("fiat_stablecoins", R.string.Market_Category_StableCoins, R.drawable.ic_usd, R.string.Market_Category_StableCoins_Description)
    object TokenizedBitcoin : MarketCategory("tokenized_bitcoin", R.string.Market_Category_TokenizedBitcoin, R.drawable.ic_coin, R.string.Market_Category_TokenizedBitcoin_Description)
    object AlgoStablecoins : MarketCategory("algo_stablecoins", R.string.Market_Category_AlgoStablecoins, R.drawable.ic_unordered_2, R.string.Market_Category_AlgoStablecoins_Description)
    object ExchangeTokens : MarketCategory("exchange_tokens", R.string.Market_Category_ExchangeTokens, R.drawable.ic_refresh, R.string.Market_Category_ExchangeTokens_Description)
}
