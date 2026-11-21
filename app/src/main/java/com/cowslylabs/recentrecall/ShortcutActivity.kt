package com.cowslylabs.recentrecall

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/** Invisible activity behind the launcher shortcut and the "Recents" launcher alias. */
class ShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val svc = RecentRecallService.instance
        if (svc != null) {
            // Short delay so this transparent activity is gone before Recents opens.
            Handler(Looper.getMainLooper()).postDelayed({ svc.openRecents() }, 150)
        } else {
            Toast.makeText(this, "Turn on the Recent-Recall accessibility service first", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
