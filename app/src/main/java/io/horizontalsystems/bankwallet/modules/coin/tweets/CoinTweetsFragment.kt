package io.horizontalsystems.bankwallet.modules.coin.tweets

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import coil.annotation.ExperimentalCoilApi
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.CellTweet
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper

@ExperimentalCoilApi
class CoinTweetsFragment : BaseFragment() {
    private val vmFactory by lazy { CoinTweetsModule.Factory(coinViewModel.fullCoin) }

    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)
    private val viewModel by viewModels<CoinTweetsViewModel> { vmFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComposeAppTheme {
                    CoinTweetsScreen(viewModel)
                }
            }
        }
    }
}

@ExperimentalCoilApi
@Composable
fun CoinTweetsScreen(viewModel: CoinTweetsViewModel) {
    val context = LocalContext.current
    val items by viewModel.itemsLiveData.observeAsState()
    when (val itemsCopy = items) {
        is DataState.Error -> {
            Log.e("AAA", "Error", itemsCopy.error)
        }
        DataState.Loading -> TODO()
        is DataState.Success -> {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                items(itemsCopy.data) { tweet: Tweet ->
                    Spacer(modifier = Modifier.height(12.dp))
                    CellTweet(tweet) {
                        val url = "https://twitter.com/${it.user.username}/status/${it.id}"
                        LinkHelper.openLinkInAppBrowser(context, url)
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 32.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ButtonSecondaryDefault(
                            title = stringResource(id = R.string.CoinPage_Twitter_ShowMore),
                            onClick = {
                                val url = "https://twitter.com/${viewModel.username}"
                                LinkHelper.openLinkInAppBrowser(context, url)
                            }
                        )
                    }
                }
            }
        }
    }
}
