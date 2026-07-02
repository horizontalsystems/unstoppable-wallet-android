package io.horizontalsystems.walletkit.core

import kotlinx.serialization.Serializable

@Serializable
enum class NavigationType {
    SlideFromBottom,
    SlideFromRight,
}