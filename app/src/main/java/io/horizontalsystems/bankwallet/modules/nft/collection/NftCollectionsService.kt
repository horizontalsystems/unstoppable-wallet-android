package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.core.managers.NoActiveAccount
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.nft.NftCollection
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update

class NftCollectionsService(
    private val nftManager: NftManager,
    private val accountRepository: NftCollectionsAccountRepository
) {
    private val _nftCollections = MutableStateFlow<DataState<List<NftCollection>>>(DataState.Loading)
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
                nftManager.getCollections(account.id)
                    .collect { collections ->
                        _nftCollections.update {
                            DataState.Success(collections)
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
