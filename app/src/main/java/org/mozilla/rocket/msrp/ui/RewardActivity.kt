package org.mozilla.rocket.msrp.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

class RewardActivity : AppCompatActivity() {

    companion object {
        fun getStartIntent(context: Context): Intent = Intent(context, RewardActivity::class.java)
    }
}