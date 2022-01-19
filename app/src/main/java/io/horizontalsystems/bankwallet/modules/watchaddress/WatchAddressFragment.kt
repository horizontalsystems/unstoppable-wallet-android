package io.horizontalsystems.bankwallet.modules.watchaddress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.unstoppabledomains.exceptions.ns.NamingServiceException
import com.unstoppabledomains.resolution.Resolution
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.ethereumkit.core.AddressValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchAddressFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    WatchAddressScreen(findNavController())
                }
            }
        }
    }
}

@Composable
fun WatchAddressScreen(navController: NavController) {
    val viewModel = viewModel<WatchAddressViewModel>(factory = WatchAddressModule.Factory())

    if (viewModel.accountCreated) {
        navController.popBackStack()
    }

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Watch_Address_Title),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Watch_Address_Watch),
                        onClick = {
                            viewModel.onClickWatch()
                        },
                        enabled = viewModel.submitEnabled
                    )
                )
            )

            val focusRequester = remember { FocusRequester() }

            HSAddressInput(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .focusRequester(focusRequester),
                onValueChange = {
                    viewModel.onEnterAddress(it)
                },
                coinCode = "ETH"
            )

            SideEffect {
                focusRequester.requestFocus()
            }
        }
    }
}

@Composable
fun HSAddressInput(modifier: Modifier, onValueChange: (Address?) -> Unit, coinCode: String) {
    val viewModel = viewModel<AddressViewModel>(factory = HSAddressInputModule.Factory(coinCode))
    val scope = rememberCoroutineScope()

    var error by remember { mutableStateOf<String?>(null) }

    FormsInput(
        modifier = modifier,
        hint = stringResource(id = R.string.Watch_Address_Hint),
        error = error,
        onValueChange = {
            scope.launch {
                val addressState = viewModel.parseAddress(it)
                error = addressState.errorOrNull?.convertedError?.localizedMessage
                onValueChange.invoke(addressState.dataOrNull)
            }
        }
    )
}

object HSAddressInputModule {
    class Factory(private val coinCode: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddressViewModel(Resolution(), coinCode) as T
        }
    }
}

class AddressViewModel(
    private val resolution: Resolution,
    private val coinCode: String
) : ViewModel() {

    suspend fun parseAddress(v1: String): DataState<Address?> = withContext(Dispatchers.IO) {
        val v = v1.trim()
        if (v.length < 40 && !v.contains(".")) {
            DataState.Success(null)
        } else {
            try {
                var addressString: String = v
                var addressDomain: String? = null

                if (v.contains(".")) {
                    if (resolution.isSupported(v)) {
                        addressString = resolution.getAddress(v, coinCode)
                        addressDomain = v
                    }
                }

                AddressValidator.validate(addressString)
                DataState.Success(Address(addressString, addressDomain))
            } catch (e: NamingServiceException) {
                DataState.Error(e)
            } catch (e: AddressValidator.AddressValidationException) {
                DataState.Error(e)
            }
        }
    }
}