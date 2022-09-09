import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.components.A1
import io.horizontalsystems.bankwallet.ui.compose.components.A2

@Composable
fun SeedPhrase(number: Int, word: String) {
    Row(
        modifier = Modifier.height(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        A1(
            text = "$number.",
            modifier = Modifier.width(32.dp),
        )
        A2(word)
    }
}
