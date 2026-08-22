package com.hinohara.aurastudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinohara.aurastudio.R
import com.hinohara.aurastudio.ui.theme.*

@Composable
fun EditorScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.editor_no_file),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.editor_no_file_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            )
        }
    }
}
