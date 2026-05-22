package com.vnt.phone

import android.content.Intent
import android.os.IBinder
import android.telecom.Call
import android.telecom.InCallService
import java.lang.ref.WeakReference

class InCallService : InCallService() {

    override fun onBind(intent: Intent): IBinder? {
        instance = this
        return super.onBind(intent)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "InCallService: Call added")

        val handle = call.details.accountHandle
        val vntHandle = BaresipService.getPhoneAccountHandle(this)

        if (handle == vntHandle) {
            Log.d(TAG, "InCallService: Identified as SIP call")
            // SIP call is already managed by ConnectionService/BaresipService
        } else {
            Log.d(TAG, "InCallService: Identified as PSTN call from $handle")
            val aor = call.details.intentExtras?.getString("aor")
            BaresipService.instance?.handleExternalCall(call, aor)
            routeToAI(call)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "InCallService: Call removed")
        BaresipService.instance?.handleExternalCallRemoved(call)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }


    private fun routeToAI(call: android.telecom.Call) {
        val callerNumber = call.details?.handle?.schemeSpecificPart ?: "unknown"
        val ryanNumber = "+966568116899"
        val bridgeHost = "192.168.10.96"
        val bridgePort = 9999
        Thread {
            try {
                val socket = java.net.Socket(bridgeHost, bridgePort)
                val out = socket.getOutputStream()
                val agent = if (callerNumber.contains(ryanNumber.takeLast(9))) "alias" else "mia"
                out.write("{\"caller\":\"$callerNumber\",\"agent\":\"$agent\"}\n".toByteArray())
                out.flush()
                android.util.Log.i("VNT", "Routed $callerNumber to $agent")
                socket.close()
            } catch (e: Exception) {
                android.util.Log.e("VNT", "Bridge error: ${e.message}")
            }
        }.start()
        // Auto-answer after 1 ring
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try { call.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY) } catch (e: Exception) {}
        }, 3000)
    }

    companion object {
        private const val TAG = "VNT"
        private var _instance = WeakReference<InCallService>(null)
        var instance: InCallService?
            get() = _instance.get()
            set(value) {
                _instance = WeakReference(value)
            }
    }
}
