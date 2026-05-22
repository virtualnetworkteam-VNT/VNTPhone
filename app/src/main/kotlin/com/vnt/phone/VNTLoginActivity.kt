package com.vnt.phone

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class VNTLoginActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val auth = getSharedPreferences("vnt_auth", MODE_PRIVATE)
        if (auth.contains("user_name")) {
            startActivity(Intent(this, MainActivity::class.java)); finish(); return
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(60,120,60,60)
            setBackgroundColor(android.graphics.Color.parseColor("#0d0d0d"))
        }
        val title = TextView(this).apply { text = "VNT"; textSize = 32f
            setTextColor(android.graphics.Color.parseColor("#6366f1"))
            gravity = android.view.Gravity.CENTER }
        val etUser = EditText(this).apply { hint = "Username"; setTextColor(-1)
            setHintTextColor(android.graphics.Color.GRAY); setPadding(20,20,20,20) }
        val etPass = EditText(this).apply { hint = "Password"; setTextColor(-1)
            setHintTextColor(android.graphics.Color.GRAY); setPadding(20,20,20,20)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val btnLogin = Button(this).apply { text = "LOGIN"
            setBackgroundColor(android.graphics.Color.parseColor("#6366f1")) }
        layout.addView(title); layout.addView(etUser); layout.addView(etPass); layout.addView(btnLogin)
        setContentView(layout)
        btnLogin.setOnClickListener {
            val u = etUser.text.toString().trim()
            val p = etPass.text.toString().trim()
            Thread {
                try {
                    val url = URL("http://192.168.10.96:8888/api/auth.php")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"; conn.doOutput = true
                    conn.outputStream.write("username=$u&password=$p".toByteArray())
                    val resp = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(resp)
                    if (json.optString("status") == "ok") {
                        auth.edit()
                            .putString("user_name", json.optString("name", u))
                            .putString("user_role", json.optString("role", "user"))
                            .apply()
                        runOnUiThread {
                            startActivity(Intent(this, MainActivity::class.java)); finish()
                        }
                    } else { runOnUiThread { Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show() } }
                } catch (e: Exception) { runOnUiThread { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() } }
            }.start()
        }
    }
}
