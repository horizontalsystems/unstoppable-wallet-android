package io.horizontalsystems.bankwallet.modules.market.top

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.R

sealed class MarketCategory(
        @StringRes val titleResId: Int,
        @DrawableRes val iconResId: Int,
        @StringRes val descriptionResId: Int
) {
    object Rated : MarketCategory(R.string.Market_Category_Rated, R.drawable.ic_chart, R.string.Market_Category_Rated_Description)
    object Blockchains : MarketCategory(R.string.Market_Category_Blockchains, R.drawable.ic_blocks, R.string.Market_Category_Blockchains_Description)
    object Privacy : MarketCategory(R.string.Market_Category_Privacy, R.drawable.ic_shield, R.string.Market_Category_Privacy_Description)
    object Scaling : MarketCategory(R.string.Market_Category_Scaling, R.drawable.ic_scale, R.string.Market_Category_Scaling_Description)
    object Infrastructure : MarketCategory(R.string.Market_Category_Infrastructure, R.drawable.ic_settings_2, R.string.Market_Category_Infrastructure_Description)
    object RiskManagement : MarketCategory(R.string.Market_Category_RiskManagement, R.drawable.ic_clipboard, R.string.Market_Category_RiskManagement_Description)
    object Oracles : MarketCategory(R.string.Market_Category_Oracles, R.drawable.ic_eye, R.string.Market_Category_Oracles_Description)
    object PredictionMarkets : MarketCategory(R.string.Market_Category_PredictionMarkets, R.drawable.ic_markets, R.string.Market_Category_PredictionMarkets_Description)
    object DefiAggregators : MarketCategory(R.string.Market_Category_DefiAggregators, R.drawable.ic_portfolio, R.string.Market_Category_DefiAggregators_Description)
    object Dexes : MarketCategory(R.string.Market_Category_Dexes, R.drawable.ic_swap_2, R.string.Market_Category_Dexes_Description)
    object Synthetics : MarketCategory(R.string.Market_Category_Synthetics, R.drawable.ic_flask, R.string.Market_Category_Synthetics_Description)
    object Metals : MarketCategory(R.string.Market_Category_Metals, R.drawable.ic_metals, R.string.Market_Category_Metals_Description)
    object Lending : MarketCategory(R.string.Market_Category_Lending, R.drawable.ic_swap_approval_2, R.string.Market_Category_Lending_Description)
    object GamingAndVr : MarketCategory(R.string.Market_Category_GamingAndVr, R.drawable.ic_game, R.string.Market_Category_GamingAndVr_Description)
    object FundRaising : MarketCategory(R.string.Market_Category_Fundraising, R.drawable.ic_download, R.string.Market_Category_Fundraising_Description)
    object InternetOfThings : MarketCategory(R.string.Market_Category_InternetOfThings, R.drawable.ic_globe, R.string.Market_Category_InternetOfThings_Description)
    object B2B : MarketCategory(R.string.Market_Category_B2B, R.drawable.ic_swap, R.string.Market_Category_B2B_Description)
    object NFT : MarketCategory(R.string.Market_Category_NFT, R.drawable.ic_user, R.string.Market_Category_NFT_Description)
    object Wallets : MarketCategory(R.string.Market_Category_Wallets, R.drawable.ic_wallet, R.string.Market_Category_Wallets_Description)
    object Staking : MarketCategory(R.string.Market_Category_Staking, R.drawable.ic_plus_circled, R.string.Market_Category_Staking_Description)
    object Stablecoins : MarketCategory(R.string.Market_Category_StableCoins, R.drawable.ic_usd, R.string.Market_Category_StableCoins_Description)
    object TokenizedBitcoin : MarketCategory(R.string.Market_Category_TokenizedBitcoin, R.drawable.ic_coin, R.string.Market_Category_TokenizedBitcoin_Description)
    object AlgoStablecoins : MarketCategory(R.string.Market_Category_AlgoStablecoins, R.drawable.ic_unordered_2, R.string.Market_Category_AlgoStablecoins_Description)
    object ExchangeTokens : MarketCategory(R.string.Market_Category_ExchangeTokens, R.drawable.ic_refresh, R.string.Market_Category_ExchangeTokens_Description)
}
