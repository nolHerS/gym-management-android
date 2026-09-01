package com.imanol.gymmanagement.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.imanol.gymmanagement.core.designsystem.theme.GymTheme

@Composable
fun GymTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    GymOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

@Composable
internal fun GymOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    label: String?,
    placeholder: String?,
    enabled: Boolean,
    isError: Boolean,
    supportingText: String?,
    singleLine: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable (() -> Unit))? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        singleLine = singleLine,
        shape = MaterialTheme.shapes.medium,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        trailingIcon = trailingIcon,
    )
}

@Preview(name = "Light")
@Composable
private fun GymTextFieldLightPreview() {
    GymTheme(darkTheme = false) {
        Column {
            GymTextField(
                value = "John Doe",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = "Name",
                placeholder = "Enter your name",
            )
            GymTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = "Disabled",
                enabled = false,
            )
            GymTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = "Email",
                isError = true,
                supportingText = "Enter a valid email address",
            )
        }
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun GymTextFieldDarkPreview() {
    GymTheme(darkTheme = true) {
        GymTextField(
            value = "Training session",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = "Session",
        )
    }
}
