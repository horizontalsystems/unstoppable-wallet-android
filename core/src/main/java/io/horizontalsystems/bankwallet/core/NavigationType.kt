package io.horizontalsystems.bankwallet.core

import kotlinx.serialization.Serializable

@Serializable
enum class NavigationType {
    SlideFromBottom,
    SlideFromRight,
}