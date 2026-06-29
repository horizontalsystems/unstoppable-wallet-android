package cash.p.terminal.modules.multiswap.providers

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cash.p.terminal.R

enum class ProviderRiskType(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
) {
    Auto(
        R.string.swap_provider_type_auto,
        R.string.swap_provider_type_auto_description,
        R.drawable.shield_check_filled_24,
    ),
    Flexible(
        R.string.swap_provider_type_flexible,
        R.string.swap_provider_type_flexible_description,
        R.drawable.thumbsup_24,
    ),
    PreCheck(
        R.string.swap_provider_type_precheck,
        R.string.swap_provider_type_precheck_description,
        R.drawable.radar_24,
    ),
    Controlled(
        R.string.swap_provider_type_controlled,
        R.string.swap_provider_type_controlled_description,
        R.drawable.ic_warning_filled_24,
    ),
}
