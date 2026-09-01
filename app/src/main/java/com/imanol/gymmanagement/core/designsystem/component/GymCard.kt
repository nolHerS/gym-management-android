package com.imanol.gymmanagement.core.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.imanol.gymmanagement.core.designsystem.theme.GymTheme

@Composable
fun GymCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            content = content,
        )
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            content = content,
        )
    }
}

@Preview(name = "Light card")
@Composable
private fun GymCardLightPreview() {
    GymTheme {
        GymCard {
        }
    }
}

@Preview(name = "Dark clickable card")
@Composable
private fun GymCardDarkPreview() {
    GymTheme(darkTheme = true) {
        GymCard(onClick = {}, enabled = false) {
        }
    }
}
