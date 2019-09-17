package org.mozilla.rocket.debugging

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

class DebugActivity : AppCompatActivity() {

    companion object {
        fun getStartIntent(context: Context) = Intent(context, DebugActivity::class.java)
    }
}