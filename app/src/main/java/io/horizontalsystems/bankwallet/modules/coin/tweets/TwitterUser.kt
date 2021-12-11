package io.horizontalsystems.bankwallet.modules.coin.tweets

import com.google.gson.annotations.SerializedName

data class TwitterUser(
    val id: String,
    val name: String,
    val username: String,
    @SerializedName("profile_image_url")
    val profileImageUrl: String,
)
