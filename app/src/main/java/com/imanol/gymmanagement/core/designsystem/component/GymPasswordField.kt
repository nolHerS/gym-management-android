package com.imanol.gymmanagement.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import com.imanol.gymmanagement.core.designsystem.theme.GymTheme

@Composable
fun GymPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    passwordVisible: Boolean = false,
    onPasswordVisibilityChange: (Boolean) -> Unit,
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
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(
                onClick = { onPasswordVisibilityChange(!passwordVisible) },
                enabled = enabled,
            ) {
                Icon(
                    imageVector = if (passwordVisible) GymVisibilityOffIcon else GymVisibilityIcon,
                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                )
            }
        },
    )
}

private val GymVisibilityIcon = ImageVector.Builder(
    name = "Visibility",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
    moveTo(12f, 4.5f)
    curveTo(7f, 4.5f, 2.7f, 7.6f, 1f, 12f)
    curveTo(2.7f, 16.4f, 7f, 19.5f, 12f, 19.5f)
    curveTo(17f, 19.5f, 21.3f, 16.4f, 23f, 12f)
    curveTo(21.3f, 7.6f, 17f, 4.5f, 12f, 4.5f)
    close()
    moveTo(12f, 17f)
    curveTo(9.2f, 17f, 7f, 14.8f, 7f, 12f)
    curveTo(7f, 9.2f, 9.2f, 7f, 12f, 7f)
    curveTo(14.8f, 7f, 17f, 9.2f, 17f, 12f)
    curveTo(17f, 14.8f, 14.8f, 17f, 12f, 17f)
    close()
    moveTo(12f, 15f)
    curveTo(13.7f, 15f, 15f, 13.7f, 15f, 12f)
    curveTo(15f, 10.3f, 13.7f, 9f, 12f, 9f)
    curveTo(10.3f, 9f, 9f, 10.3f, 9f, 12f)
    curveTo(9f, 13.7f, 10.3f, 15f, 12f, 15f)
    close()
}.build()

private val GymVisibilityOffIcon = ImageVector.Builder(
    name = "VisibilityOff",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
    moveTo(2f, 4.3f)
    lineTo(3.3f, 3f)
    lineTo(21f, 20.7f)
    lineTo(19.7f, 22f)
    lineTo(16.5f, 18.8f)
    curveTo(15.1f, 19.3f, 13.6f, 19.5f, 12f, 19.5f)
    curveTo(7f, 19.5f, 2.7f, 16.4f, 1f, 12f)
    curveTo(1.7f, 10.2f, 2.8f, 8.5f, 4.2f, 7.2f)
    lineTo(2f, 4.3f)
    close()
    moveTo(9.5f, 12f)
    curveTo(9.5f, 13.4f, 10.6f, 14.5f, 12f, 14.5f)
    curveTo(12.6f, 14.5f, 13.1f, 14.3f, 13.5f, 14f)
    lineTo(10f, 10.5f)
    curveTo(9.7f, 10.9f, 9.5f, 11.4f, 9.5f, 12f)
    close()
    moveTo(12f, 4.5f)
    curveTo(17f, 4.5f, 21.3f, 7.6f, 23f, 12f)
    curveTo(22.3f, 13.8f, 21.2f, 15.5f, 19.8f, 16.8f)
    lineTo(16.3f, 13.3f)
    curveTo(16.4f, 12.9f, 16.5f, 12.5f, 16.5f, 12f)
    curveTo(16.5f, 9.5f, 14.5f, 7.5f, 12f, 7.5f)
    curveTo(11.5f, 7.5f, 11.1f, 7.6f, 10.7f, 7.7f)
    lineTo(8.4f, 5.4f)
    curveTo(9.5f, 4.8f, 10.7f, 4.5f, 12f, 4.5f)
    close()
}.build()

@Preview(name = "Password states")
@Composable
private fun GymPasswordFieldPreview() {
    GymTheme {
        Column {
            GymPasswordField(
                value = "secret",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = "Password",
                onPasswordVisibilityChange = {},
            )
            GymPasswordField(
                value = "secret",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = "Visible password",
                passwordVisible = true,
                onPasswordVisibilityChange = {},
            )
        }
    }
}
