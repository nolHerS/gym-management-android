package com.imanol.gymmanagement.core.designsystem.component

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.imanol.gymmanagement.core.designsystem.theme.GymTheme

@Composable
fun GymLoading(
    modifier: Modifier = Modifier,
    contentDescription: String = "Cargando",
) {
    CircularProgressIndicator(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
    )
}

@Preview(name = "Light loading")
@Composable
private fun GymLoadingLightPreview() {
    GymTheme {
        GymLoading()
    }
}

@Preview(name = "Dark loading")
@Composable
private fun GymLoadingDarkPreview() {
    GymTheme(darkTheme = true) {
        GymLoading()
    }
}
