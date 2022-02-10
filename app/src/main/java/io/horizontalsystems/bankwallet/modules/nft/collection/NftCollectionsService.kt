package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.core.managers.NoActiveAccount
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.nft.NftAsset
import io.horizontalsystems.bankwallet.modules.nft.NftCollection
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class NftCollectionsService(
    private val nftManager: NftManager,
    private val accountRepository: NftCollectionsAccountRepository
) {
    private val _nftCollections = MutableStateFlow<DataState<Pair<List<NftCollection>, List<NftAsset>>>>(DataState.Loading)
    val nftCollections = _nftCollections.asStateFlow()

    private val disposables = CompositeDisposable()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var handleActiveAccountJob: Job? = null

    fun start() {
        coroutineScope.launch {
            accountRepository.account.collect {
                handleAccount(it?.first, it?.second)
            }
        }

        accountRepository.start()
    }

    private fun handleAccount(account: Account?, address: Address?) {
        handleActiveAccountJob?.cancel()

        if (account != null && address != null) {
            handleActiveAccountJob = coroutineScope.launch {
                combine(
                    flow = nftManager.getCollections(account.id),
                    flow2 = nftManager.getAssets(account.id),
                    transform = { collections, assets ->
                        Pair(collections, assets)
                    })
                    .collect { (collections, assets) ->
                        _nftCollections.update {
                            DataState.Success(Pair(collections, assets))
                        }
                    }
            }

            coroutineScope.launch {
                nftManager.refresh(account, address)
            }
        } else {
            _nftCollections.update {
                DataState.Error(NoActiveAccount())
            }
        }
    }

    suspend fun refresh() {
        accountRepository.account.value?.let { (account, address) ->
            nftManager.refresh(account, address)
        }
    }

    fun stop() {
        accountRepository.stop()
        disposables.clear()
        coroutineScope.cancel()
    }
}
