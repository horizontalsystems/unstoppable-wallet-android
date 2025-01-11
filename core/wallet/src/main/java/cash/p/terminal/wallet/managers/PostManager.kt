package cash.p.terminal.wallet.managers

import cash.p.terminal.wallet.models.Post
import cash.p.terminal.wallet.providers.CryptoCompareProvider
import io.reactivex.Single

class PostManager(
    private val provider: CryptoCompareProvider
) {
    fun postsSingle(): Single<List<Post>> {
        return provider.postsSingle()
    }
}
