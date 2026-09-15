package com.earthnow.app.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.earthnow.app.R

/**
 * Lets the user paste AI API keys without rebuilding the app. Values are
 * encrypted with the Android Keystore before being persisted.
 */
@Composable
fun AiKeysEditor(
    maskedDeepSeek: String,
    maskedOpenAi: String,
    maskedGemini: String,
    baseUrl: String,
    onSave: (provider: String, value: String) -> Unit,
    onClear: (provider: String) -> Unit,
    onSaveBaseUrl: (String) -> Unit
) {
    Column {
        Text(
            stringResource(R.string.ai_keys_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        AiKeyRow(
            label = stringResource(R.string.ai_key_deepseek),
            saved = maskedDeepSeek,
            onSave = { onSave("deepseek", it) },
            onClear = { onClear("deepseek") }
        )
        AiKeyRow(
            label = stringResource(R.string.ai_key_openai),
            saved = maskedOpenAi,
            onSave = { onSave("openai", it) },
            onClear = { onClear("openai") }
        )
        AiKeyRow(
            label = stringResource(R.string.ai_key_gemini),
            saved = maskedGemini,
            onSave = { onSave("gemini", it) },
            onClear = { onClear("gemini") }
        )

        var baseUrlInput by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(
            value = baseUrlInput,
            onValueChange = { baseUrlInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.ai_key_base_url)) },
            placeholder = { Text("https://api.openai.com") },
            singleLine = true,
            supportingText = {
                Text(
                    if (baseUrl.isNotBlank()) stringResource(R.string.ai_key_saved, baseUrl)
                    else stringResource(R.string.ai_key_none),
                    style = MaterialTheme.typography.labelSmall
                )
            },
            trailingIcon = {
                TextButton(onClick = {
                    onSaveBaseUrl(baseUrlInput)
                    baseUrlInput = ""
                }) { Text(stringResource(R.string.ai_save)) }
            }
        )
    }
}

@Composable
private fun AiKeyRow(
    label: String,
    saved: String,
    onSave: (String) -> Unit,
    onClear: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.padding(bottom = 6.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            supportingText = {
                Text(
                    if (saved.isNotBlank()) stringResource(R.string.ai_key_saved, saved)
                    else stringResource(R.string.ai_key_none),
                    style = MaterialTheme.typography.labelSmall
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            stringResource(if (visible) R.string.ai_key_hide else R.string.ai_key_show)
                        )
                    }
                    TextButton(onClick = {
                        if (input.isNotBlank()) {
                            onSave(input.trim())
                            input = ""
                        }
                    }) { Text(stringResource(R.string.ai_save)) }
                }
            }
        )
        if (saved.isNotBlank()) {
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(stringResource(R.string.ai_clear_key), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}