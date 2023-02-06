package io.horizontalsystems.marketkit.managers

import io.horizontalsystems.marketkit.models.Post
import io.horizontalsystems.marketkit.providers.CryptoCompareProvider
import io.reactivex.Single

class PostManager(
    private val provider: CryptoCompareProvider
) {
    fun postsSingle(): Single<List<Post>> {
        return provider.postsSingle()
    }
}
