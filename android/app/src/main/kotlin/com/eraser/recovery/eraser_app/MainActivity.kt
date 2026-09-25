package com.eraser.recovery.eraser_app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.annotation.NonNull
import com.eraser.recovery.domain.vpn.VpnManager
import com.eraser.recovery.vpn.EraserVpnService
import dagger.hilt.android.AndroidEntryPoint
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FlutterActivity() {

    @Inject
    lateinit var vpnManager: VpnManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var vpnStatusEventSink: EventChannel.EventSink? = null
    private var blockedContentEventSink: EventChannel.EventSink? = null

    companion object {
        private const val VPN_CHANNEL = "com.eraser.recovery/vpn"
        private const val VPN_STATUS_STREAM = "com.eraser.recovery/vpn_status"
        private const val BLOCKED_CONTENT_STREAM = "com.eraser.recovery/blocked_content"
        private const val VPN_REQUEST_CODE = 100
    }

    private val blockedContentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.eraser.CONTENT_BLOCKED") {
                val domain = intent.getStringExtra("domain") ?: ""
                val url = intent.getStringExtra("url") ?: ""

                blockedContentEventSink?.success(mapOf(
                    "domain" to domain,
                    "url" to url,
                    "timestamp" to System.currentTimeMillis()
                ))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register blocked content receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                blockedContentReceiver,
                IntentFilter("com.eraser.CONTENT_BLOCKED"),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                blockedContentReceiver,
                IntentFilter("com.eraser.CONTENT_BLOCKED")
            )
        }
    }

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Method Channel for VPN control
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, VPN_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "isVpnPermissionGranted" -> {
                    result.success(vpnManager.hasVpnPermission())
                }

                "requestVpnPermission" -> {
                    requestVpnPermission(result)
                }

                "startVpn" -> {
                    startVpnService(result)
                }

                "stopVpn" -> {
                    stopVpnService(result)
                }

                "isVpnRunning" -> {
                    result.success(vpnManager.isVpnRunning())
                }

                "getVpnStatistics" -> {
                    val stats = vpnManager.getStatistics()
                    result.success(mapOf(
                        "totalBlocked" to stats.totalBlockedAttempts,
                        "todayBlocked" to stats.todayBlockedAttempts,
                        "weekBlocked" to stats.weekBlockedAttempts
                    ))
                }

                "updateBlocklist" -> {
                    scope.launch {
                        try {
                            vpnManager.updateBlocklist()
                            result.success(true)
                        } catch (e: Exception) {
                            result.error("UPDATE_FAILED", e.message, null)
                        }
                    }
                }

                else -> {
                    result.notImplemented()
                }
            }
        }

        // Event Channel for VPN status updates
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, VPN_STATUS_STREAM)
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    vpnStatusEventSink = events

                    scope.launch {
                        vpnManager.isVpnActiveFlow.collectLatest { isActive ->
                            vpnStatusEventSink?.success(isActive)
                        }
                    }
                }

                override fun onCancel(arguments: Any?) {
                    vpnStatusEventSink = null
                }
            })

        // Event Channel for blocked content notifications
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, BLOCKED_CONTENT_STREAM)
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    blockedContentEventSink = events
                }

                override fun onCancel(arguments: Any?) {
                    blockedContentEventSink = null
                }
            })
    }

    private fun requestVpnPermission(result: MethodChannel.Result) {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
            pendingVpnPermissionResult = result
        } else {
            // Permission already granted
            result.success(true)
        }
    }

    private var pendingVpnPermissionResult: MethodChannel.Result? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VPN_REQUEST_CODE) {
            val granted = resultCode == Activity.RESULT_OK
            pendingVpnPermissionResult?.success(granted)
            pendingVpnPermissionResult = null
        }
    }

    private fun startVpnService(result: MethodChannel.Result) {
        scope.launch {
            try {
                vpnManager.startVpn()
                result.success(true)
            } catch (e: Exception) {
                result.error("VPN_START_FAILED", e.message, null)
            }
        }
    }

    private fun stopVpnService(result: MethodChannel.Result) {
        scope.launch {
            try {
                vpnManager.stopVpn()
                result.success(true)
            } catch (e: Exception) {
                result.error("VPN_STOP_FAILED", e.message, null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(blockedContentReceiver)
        scope.cancel()
    }
}
