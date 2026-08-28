package com.aurastudio.ui.screens.splash

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aurastudio.R
import com.aurastudio.ui.theme.LocalIsAppDark
import kotlinx.coroutines.delay

private const val MIN_SPLASH_MILLIS = 1200L

/** Permissions the app needs at startup (filtered to those still missing). */
fun missingPermissions(context: Context): List<String> = buildList {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        add(Manifest.permission.READ_EXTERNAL_STORAGE)
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/**
 * Brand splash: shows the app logo (matching the active light/dark icon), requests
 * any pending runtime permissions once, then invokes [onFinished].
 */
@Composable
fun SplashScreen(
    useDarkLogo: Boolean,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onFinished()
    }

    LaunchedEffect(Unit) {
        delay(MIN_SPLASH_MILLIS)
        val missing = missingPermissions(context)
        if (missing.isEmpty()) onFinished() else permissionLauncher.launch(missing.toTypedArray())
    }

    SplashContent(useDarkLogo = useDarkLogo)
}

@Composable
private fun SplashContent(useDarkLogo: Boolean) {
    val logoDark = useDarkLogo || LocalIsAppDark.current
    val primary = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val tagline = stringResource(R.string.dashboard_tagline)
    val appName = stringResource(R.string.app_name)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            val pulseTransition = rememberInfiniteTransition(label = "logoPulse")
            val pulse by pulseTransition.animateFloat(
                initialValue = 0.94f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "pulse",
            )

            Image(
                painter = painterResource(if (logoDark) R.drawable.ic_logo_dark else R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(132.dp)
                    .alpha(pulse),
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = appName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                color = primary,
                strokeWidth = 3.dp,
            )
        }
    }
}