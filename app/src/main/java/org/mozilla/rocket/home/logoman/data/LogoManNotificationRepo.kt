package org.mozilla.rocket.home.logoman.data

import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.utils.FirebaseHelper

class LogoManNotificationRepo {

    fun getNotification(): Notification? =
            FirebaseHelper.getFirebase().getRcString(STR_LOGO_MAN_NOTIFICATION)
                    .takeIf { it.isNotEmpty() }
                    ?.jsonStringToNotification()

    companion object {
        private const val STR_LOGO_MAN_NOTIFICATION = "str_logo_man_notification"
    }
}

data class Notification(
    val title: String,
    val subtitle: String
)

private fun String.jsonStringToNotification(): Notification? {
    return try {
        val jsonObject = JSONObject(this)
        Notification(
            jsonObject.getString("title"),
            jsonObject.getString("subtitle")
        )
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}