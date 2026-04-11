package com.ai.assistance.operit.terminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.terminal.TerminalEnv

@Composable
fun InputBar(env: TerminalEnv) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E141B))
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = env.command,
            onValueChange = env::onCommandChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("What should I do next...", color = Color(0xFF7F8C9A)) },
            singleLine = true,
            textStyle = TextStyle(color = Color(0xFFF2F7FF)),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFFF2F7FF),
                unfocusedTextColor = Color(0xFFF2F7FF),
                focusedContainerColor = Color(0xFF131B24),
                unfocusedContainerColor = Color(0xFF131B24),
                cursorColor = Color(0xFFB6F09C),
                focusedBorderColor = Color(0xFF4E7BFF),
                unfocusedBorderColor = Color(0xFF35506B),
                focusedPlaceholderColor = Color(0xFF7F8C9A),
                unfocusedPlaceholderColor = Color(0xFF7F8C9A)
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                env.onSendInput(env.command, true)
            })
        )
        Button(
            onClick = { env.onSendInput(env.command, true) },
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Text("▶", style = MaterialTheme.typography.titleMedium)
        }
    }
}
