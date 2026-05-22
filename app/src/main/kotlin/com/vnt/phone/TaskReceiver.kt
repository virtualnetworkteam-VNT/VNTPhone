package com.vnt.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class TaskReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.d(TAG, "TaskReceiver: received intent ${intent.action}")

        when (intent.action) {

            "com.vnt.phone.REGISTER", "com.vnt.phone.UNREGISTER" -> {
                var aor = intent.getStringExtra("aor")
                if (aor == null) {
                    Log.i(TAG, "TaskReceiver: 'aor' extra is missing")
                    return
                }
                if (!aor.startsWith("sip:"))
                    aor = "sip:$aor"
                val ua = UserAgent.ofAor(aor)
                if (ua == null) {
                    Log.i(TAG, "TaskReceiver: user agent of AoR $aor is not found")
                    return
                }
                val acc = ua.account
                if (intent.action == "com.vnt.phone.REGISTER") {
                    Log.d(TAG, "TaskReceiver: registering $aor")
                    Api.account_set_regint(acc.accp, REGISTRATION_INTERVAL)
                    Api.ua_register(ua.uap)
                    acc.regint = Api.account_regint(acc.accp)
                    Account.saveAccounts()
                } else {
                    Log.d(TAG, "TaskReceiver: un-registering $aor")
                    Api.account_set_regint(acc.accp, 0)
                    Api.ua_unregister(ua.uap)
                    acc.regint = Api.account_regint(acc.accp)
                    Account.saveAccounts()
                }
            }

            "com.vnt.phone.QUIT" -> {
                Log.d(TAG, "TaskReceiver: quiting")
                val vntService = Intent(context, BaresipService::class.java)
                if (BaresipService.isServiceRunning) {
                    vntService.action = "Stop"
                    ContextCompat.startForegroundService(context, vntService)
                }
            }

        }
    }

}
