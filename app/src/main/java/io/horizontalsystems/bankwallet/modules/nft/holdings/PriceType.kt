package io.horizontalsystems.bankwallet.modules.nft.holdings

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle

enum class PriceType(override val title: TranslatableString) : WithTranslatableTitle {
    LastSale(TranslatableString.ResString(R.string.Nfts_PriceType_LastSale)),
    Days7(TranslatableString.ResString(R.string.Nfts_PriceType_Days_7)),
    Days30(TranslatableString.ResString(R.string.Nfts_PriceType_Days_30))
}