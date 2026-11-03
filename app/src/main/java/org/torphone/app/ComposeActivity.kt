package org.torphone.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.torphone.app.vpn.TorPhoneVpnService

enum class ProofVpnState {
    DISCONNECTED, CONNECTING, CONNECTED, FAILED
}

class ProofStatusViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProofVpnState.DISCONNECTED)
    val state: StateFlow<ProofVpnState> = _state

    fun update(value: ProofVpnState) {
        _state.value = value
    }
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
        statusModel = ViewModelProvider(this)[ProofStatusViewModel::class.java]
        statusModel.update(resolveState(TorPhoneVpnService.getLastStatus()))

        setContent {
            val model: ProofStatusViewModel = viewModel()
            val state by model.state.collectAsStateWithLifecycle()
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Tor-phone", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(20.dp))
                    Text(state.name, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        val intent = Intent(this@ComposeActivity, MainActivity::class.java).apply {
                            action = MainActivity.ACTION_START_VPN_FROM_COMPOSE
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(intent)
                    }) {
                        Text("Start VPN")
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(TorPhoneVpnService.ACTION_STATUS)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
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
        getString(R.string.status_engine_running_unverified) -> ProofVpnState.CONNECTED
        getString(R.string.status_engine_failed),
        getString(R.string.status_engine_init_failed),
        getString(R.string.status_socket_protection_failed),
        getString(R.string.engine_initialization_failed),
        getString(R.string.vpn_permission_denied),
        getString(R.string.status_vpn_permission_revoked) -> ProofVpnState.FAILED
        else -> ProofVpnState.DISCONNECTED
    }
}
