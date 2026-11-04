package org.torphone.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.torphone.app.vpn.TorPhoneVpnService

enum class ProofVpnState { DISCONNECTED, CONNECTING, RUNNING_UNVERIFIED, FAILED }

class ProofStatusViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProofVpnState.DISCONNECTED)
    val state: StateFlow<ProofVpnState> = _state
    fun update(value: ProofVpnState) { _state.value = value }
}

class ComposeActivity : ComponentActivity() {
    private lateinit var statusModel: ProofStatusViewModel
    private var receiverRegistered = false

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TorPhoneVpnService.ACTION_STATUS) {
                statusModel.update(resolveState(intent.getStringExtra(TorPhoneVpnService.EXTRA_STATUS)))
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePrefs.apply(this)
        statusModel = ViewModelProvider(this)[ProofStatusViewModel::class.java]
        statusModel.update(resolveState(TorPhoneVpnService.getLastStatus()))

        setContent {
            val model: ProofStatusViewModel = viewModel()
            val state by model.state.collectAsStateWithLifecycle()
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
                surface = Color(0xFFFFFFFF),
                onBackground = Color(0xFF182030),
                onSurface = Color(0xFF182030)
            )
            MaterialTheme(colorScheme = scheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("TOR / PHONE", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = {
                                ThemePrefs.toggle(this@ComposeActivity)
                                recreate()
                            }) { Text(if (dark) "☀" else "☾", style = MaterialTheme.typography.titleLarge) }
                        }
                        Column {
                            Text("Privacy, under your control.", style = MaterialTheme.typography.headlineMedium)
                            Text("لوحة تحكم اتصال Tor وVPN", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f))
                        }
                        val connectedColor by animateColorAsState(
                            if (state == ProofVpnState.RUNNING_UNVERIFIED) Color(0xFFF3B95F)
                            else if (state == ProofVpnState.FAILED) Color(0xFFFF647C)
                            else MaterialTheme.colorScheme.primary, label = "statusColor"
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text("PROTECTION STATUS", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    when (state) {
                                        ProofVpnState.CONNECTING -> "جارٍ الاتصال..."
                                        ProofVpnState.RUNNING_UNVERIFIED -> "Tor Engine يعمل"
                                        ProofVpnState.FAILED -> "فشل الاتصال"
                                        else -> "غير متصل"
                                    },
                                    style = MaterialTheme.typography.headlineSmall, color = connectedColor
                                )
                                Text(
                                    when (state) {
                                        ProofVpnState.CONNECTING -> "Establishing Tor circuit"
                                        ProofVpnState.RUNNING_UNVERIFIED -> "Engine is running; IP routing is not verified"
                                        ProofVpnState.FAILED -> "Check logs for details"
                                        else -> "Ready to connect"
                                    },
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f)
                                )
                            }
                        }
                        Button(
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            onClick = {
                                val intent = Intent(this@ComposeActivity, MainActivity::class.java).apply {
                                    action = MainActivity.ACTION_START_VPN_FROM_COMPOSE
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                }
                                startActivity(intent)
                            }
                        ) { Text("Open connection controls") }
                        Text("الحالة المعروضة تخص Engine فقط؛ لا تعني نجاح اختبار Public IP أو منع التسرب.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                    }
                }
            }
        }
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
        statusModel.update(resolveState(TorPhoneVpnService.getLastStatus()))
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(statusReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    private fun resolveState(status: String?): ProofVpnState = when (status) {
        getString(R.string.status_engine_starting) -> ProofVpnState.CONNECTING
        getString(R.string.status_engine_running_unverified) -> ProofVpnState.RUNNING_UNVERIFIED
        getString(R.string.status_engine_failed),
        getString(R.string.status_engine_init_failed),
        getString(R.string.status_socket_protection_failed),
        getString(R.string.engine_initialization_failed),
        getString(R.string.vpn_permission_denied),
        getString(R.string.status_vpn_permission_revoked) -> ProofVpnState.FAILED
        else -> ProofVpnState.DISCONNECTED
    }
}
