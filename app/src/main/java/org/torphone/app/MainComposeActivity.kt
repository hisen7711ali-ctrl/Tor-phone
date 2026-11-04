package org.torphone.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.torphone.app.vpn.TorPhoneVpnService

class MainComposeActivity : ComponentActivity() {
    private var currentStatus by mutableStateOf("")
    private var bytesSent by mutableLongStateOf(0L)
    private var bytesReceived by mutableLongStateOf(0L)
    private var exitIp by mutableStateOf("")
    private var stickyExit by mutableStateOf(false)
    private var receiverRegistered = false

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != TorPhoneVpnService.ACTION_STATUS) return
            currentStatus = intent.getStringExtra(TorPhoneVpnService.EXTRA_STATUS).orEmpty()
            bytesSent = intent.getLongExtra(TorPhoneVpnService.EXTRA_SENT, 0L)
            bytesReceived = intent.getLongExtra(TorPhoneVpnService.EXTRA_RECEIVED, 0L)
            exitIp = intent.getStringExtra(TorPhoneVpnService.EXTRA_EXIT_IP).orEmpty()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePrefs.apply(this)
        currentStatus = TorPhoneVpnService.getLastStatus().orEmpty()
        exitIp = TorPhoneVpnService.getLastVerifiedExitIp()
        stickyExit = getSharedPreferences("torphone_preferences", MODE_PRIVATE)
            .getBoolean("privacy_sticky_exit", false)

        setContent {
            val dark = ThemePrefs.isDark(this)
            val scheme = if (dark) darkColorScheme(
                primary = Color(0xFF9B83FF),
                secondary = Color(0xFF32D6C2),
                background = Color(0xFF0A0E1A),
                surface = Color(0xFF1A1F35),
                onBackground = Color(0xFFF4F7FF),
                onSurface = Color(0xFFF4F7FF)
            ) else lightColorScheme(
                primary = Color(0xFF6C4CE6),
                secondary = Color(0xFF159E91),
                background = Color(0xFFF6F8FC),
                surface = Color.White,
                onBackground = Color(0xFF182030),
                onSurface = Color(0xFF182030)
            )
            MaterialTheme(colorScheme = scheme) {
                DashboardScreen(
                    dark = dark,
                    status = currentStatus,
                    sent = bytesSent,
                    received = bytesReceived,
                    exitIp = exitIp,
                    sticky = stickyExit,
                    onTheme = { ThemePrefs.toggle(this); recreate() },
                    onConnect = { openConnectionControls() },
                    onStop = { stopVpn() },
                    onSettings = { startActivity(Intent(this, PrivacySettingsActivity::class.java)) },
                    onSplit = { startActivity(Intent(this, SplitTunnelingActivity::class.java)) },
                    onBridges = { startActivity(Intent(this, PrivacySettingsActivity::class.java)) },
                    onSticky = {
                        val next = !stickyExit
                        stickyExit = next
                        getSharedPreferences("torphone_preferences", MODE_PRIVATE).edit()
                            .putBoolean("privacy_sticky_exit", next)
                            .apply()
                        CircuitRotationScheduler.apply(this)
                    },
                    statusConnecting = getString(R.string.status_engine_starting),
                    statusUnverified = getString(R.string.status_engine_running_unverified),
                    statusVerified = getString(R.string.status_verified_protected),
                    statusFailed = getString(R.string.status_engine_failed)
                )
            }
        }
    }

    private fun openConnectionControls() {
        val permission = VpnService.prepare(this)
        if (permission == null) {
            launchVpnService()
        } else {
            startActivityForResult(permission, 7102)
        }
    }

    private fun launchVpnService() {
        val intent = Intent(this, TorPhoneVpnService::class.java).apply {
            action = TorPhoneVpnService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
    }

    @Deprecated("Deprecated by Android; retained for the VPN consent activity result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 7102 && resultCode == RESULT_OK) launchVpnService()
    }

    private fun stopVpn() {
        val intent = Intent(this, TorPhoneVpnService::class.java).apply {
            action = TorPhoneVpnService.ACTION_STOP
        }
        startService(intent)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(TorPhoneVpnService.ACTION_STATUS)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("DEPRECATION")
            registerReceiver(statusReceiver, filter)
        }
        receiverRegistered = true
        currentStatus = TorPhoneVpnService.getLastStatus().orEmpty()
        exitIp = TorPhoneVpnService.getLastVerifiedExitIp()
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(statusReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }
}

@Composable
private fun DashboardScreen(
    dark: Boolean,
    status: String,
    sent: Long,
    received: Long,
    exitIp: String,
    sticky: Boolean,
    onTheme: () -> Unit,
    onConnect: () -> Unit,
    onStop: () -> Unit,
    onSettings: () -> Unit,
    onSplit: () -> Unit,
    onBridges: () -> Unit,
    onSticky: () -> Unit,
    statusConnecting: String,
    statusUnverified: String,
    statusVerified: String,
    statusFailed: String
) {
    val connected = status == statusVerified || status == statusUnverified
    val connecting = status == statusConnecting
    val verifying = status == statusUnverified
    val failed = status == statusFailed
    val protected = status == statusVerified
    val accent = when {
        protected -> Color(0xFF24C99A)
        verifying || connecting -> Color(0xFFF0B34F)
        failed -> Color(0xFFFF647C)
        else -> Color(0xFF7C5CFF)
    }
    val pulseTransition = rememberInfiniteTransition(label = "connectPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (connecting) 1.055f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val buttonScale by animateFloatAsState(if (connecting) pulse else 1f, label = "buttonScale")

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("TOR / PHONE", color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("Privacy, under your control.", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onTheme) { Text(if (dark) "☀" else "☾", style = MaterialTheme.typography.headlineSmall) }
            TextButton(onClick = onSettings) { Text("⚙", style = MaterialTheme.typography.headlineSmall) }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("SECURE CONNECTION", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f))
            Spacer(Modifier.height(18.dp))
            Surface(
                onClick = { if (!connecting) { if (connected) onStop() else onConnect() } },
                modifier = Modifier.size(218.dp).scale(buttonScale),
                shape = CircleShape,
                color = accent,
                shadowElevation = 14.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.background(
                    Brush.linearGradient(listOf(accent, accent.copy(alpha = .72f)))
                )) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (connecting || verifying) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(34.dp), strokeWidth = 3.dp)
                        } else {
                            Text(if (connected) "✓" else "⏻", color = Color.White,
                                style = MaterialTheme.typography.displayMedium)
                        }
                        Text(
                            when {
                                protected -> "PROTECTED"
                                verifying -> "VERIFYING"
                                connecting -> "CONNECTING"
                                failed -> "FAILED"
                                connected -> "ENGINE RUNNING"
                                else -> "CONNECT"
                            },
                            color = Color.White, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(if (connected) "Tap to disconnect" else "Connect through Tor",
                            color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                when {
                    protected -> "محمي - Verified Tor Exit"
                    verifying -> "Tor Engine يعمل - جارٍ التحقق"
                    connecting -> "جارٍ الاتصال..."
                    failed -> "فشل الاتصال"
                    else -> "غير متصل"
                },
                color = accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                when {
                    protected -> "Traffic is routed through a verified Tor exit"
                    verifying -> "Checking Tor exit status"
                    connecting -> "Establishing Tor circuit"
                    failed -> "Check logs for details"
                    else -> "Ready to connect"
                },
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            if (protected && exitIp.isNotBlank()) {
                Text("Status: Protected · Tor Exit: $exitIp",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("LIVE TRAFFIC", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TrafficValue("↑ Upload", formatBytes(sent))
                    TrafficValue("↓ Download", formatBytes(received))
                }
                Text("Counters from Onionmasq tunnel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f),
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        Text("QUICK CONTROLS", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f),
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        QuickControl("Split Tunneling", "اختر التطبيقات التي تتجاوز Tor", "›", onSplit)
        QuickControl("Bridges · obfs4", "إعداد Bridge lines لتجاوز DPI", "›", onBridges)
        QuickControl("Sticky Exit", if (sticky) "مفعّل · إيقاف التدوير المجدول" else "غير مفعّل · إعداد تدوير الدوائر", if (sticky) "ON" else "OFF", onSticky)
        Text("ملاحظة: Sticky Exit يوقف التدوير المجدول فقط؛ ولا يضمن ثبات Public IP لأن MaxCircuitDirtiness غير متاح عبر JNI الحالي.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .58f),
            style = MaterialTheme.typography.bodySmall)
        Button(onClick = onSettings, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Privacy Settings")
        }
    }
}

@Composable
private fun TrafficValue(label: String, value: String) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f),
            style = MaterialTheme.typography.bodySmall)
        Text(value, color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuickControl(title: String, detail: String, trailing: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(detail, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f),
                    style = MaterialTheme.typography.bodySmall)
            }
            Text(trailing, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatBytes(value: Long): String {
    if (value < 1024L) return "$value B"
    val kb = value / 1024.0
    if (kb < 1024.0) return String.format(java.util.Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(java.util.Locale.US, "%.1f MB", mb)
    return String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0)
}
