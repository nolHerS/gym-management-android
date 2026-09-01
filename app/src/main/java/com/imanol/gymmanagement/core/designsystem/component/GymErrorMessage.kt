package com.imanol.gymmanagement.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.imanol.gymmanagement.core.designsystem.theme.GymTheme

@Composable
fun GymErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Polite
        },
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Preview(name = "Light error")
@Composable
private fun GymErrorMessageLightPreview() {
    GymTheme {
        GymErrorMessage(message = "Unable to load your data.")
    }
}

@Preview(name = "Dark error")
@Composable
private fun GymErrorMessageDarkPreview() {
    GymTheme(darkTheme = true) {
        GymErrorMessage(message = "Please check your connection and try again.")
    }
}
