package io.horizontalsystems.bankwallet.modules.coin.majorholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.databinding.ViewHolderCoinMajorHoldersPieComposeBinding
import io.horizontalsystems.bankwallet.modules.coin.MajorHolderItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.SemiCircleChart
import kotlin.math.min

class CoinMajorHoldersPieAdapter(private val holders: List<MajorHolderItem.Item>) :
    RecyclerView.Adapter<ViewHolderItem>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderItem {
        return ViewHolderItem(
            ViewHolderCoinMajorHoldersPieComposeBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount() = 1

    override fun onBindViewHolder(holder: ViewHolderItem, position: Int) {
        holder.bind(holders)
    }
}

class ViewHolderItem(private val binding: ViewHolderCoinMajorHoldersPieComposeBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(holders: List<MajorHolderItem.Item>) {
        val portionMajor = min(holders.sumOf { it.share }.toFloat(), 100f)
        val portionRest = 100 - portionMajor

        binding.root.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                ComposeAppTheme {
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        SemiCircleChart(
                            modifier = Modifier.padding(horizontal = 32.dp),
                            percentValues = listOf(portionMajor, portionRest),
                            title = App.numberFormatter.format(portionMajor, 0, 2, suffix = "%")
                        )

                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, end = 32.dp, top = 12.dp),
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            text = stringResource(R.string.CoinPage_MajorHolders_InTopWallets),
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.subhead1
                        )

                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 38.dp),
                            textAlign = TextAlign.Start,
                            overflow = TextOverflow.Ellipsis,
                            text = stringResource(R.string.CoinPage_MajorHolders_Description),
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.subhead2
                        )
                    }
                }
            }
        }
    }
}
