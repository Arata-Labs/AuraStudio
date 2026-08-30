package com.aurastudio.ui.screens.terminal

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.aurastudio.R

@Composable
fun TerminalScreen(
    initialCommand: String?,
    scaffoldPadding: PaddingValues
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.terminal_screen_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.terminal_screen_desc),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, "termux://open".toUri()).setClassName(
                    "com.aurastudio",
                    "com.termux.app.TermuxActivity"
                )
                if (initialCommand != null) {
                    intent.putExtra("com.aurastudio.app.extra.initial_command", initialCommand)
                }
                context.startActivity(intent)
            }
        ) {
            Text(stringResource(R.string.terminal_open))
        }
    }
}