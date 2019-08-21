object Versions {
    const val min_sdk = 21
    const val target_sdk = 28
    const val compile_sdk = 28
    const val build_tools = "28.0.3"
    const val version_code = 1
    const val version_name = "2.0.0"
    const val android_gradle_plugin = "3.4.2"
    const val gms_oss_licenses_plugin = "0.9.3"
    const val support = "1.0.0"
    const val appcompat = "1.0.2"
    const val material = "1.0.0"
    const val cardview = "1.0.0"
    const val recyclerview = "1.0.0"
    const val constraint = "1.1.3"
    const val viewpager2 = "1.0.0-beta03"
    const val preference = "1.0.0"
    const val arch_core = "2.0.1"
    const val arch_work = "2.0.1"
    const val lifecycle = "2.0.0"
    const val viewmodel_ktx = "2.2.0-alpha02"
    const val room = "2.1.0"
    const val glide = "4.0.0"
    const val kotlin = "1.3.41"
    const val ktlint = "0.32.0"
    const val gms = "11.8.0"
    const val navigation = "2.0.0"
    const val findbugs = "3.0.1"
    const val lottie = "2.8.0"
    const val leakcanary = "1.6.2"
    const val android_components = "0.52.0"
    const val adjust = "4.11.4"
    const val annotation = "1.1.0"
    const val junit = "4.12"
    const val mockito = "3.0.0"
    const val json = "20180813"
    const val robolectric = "4.3"
    const val espresso = "3.2.0"
    const val test_core = "1.1.0"
    const val test_ext = "1.1.1"
    const val test_runner = "1.2.0"
    const val uiautomator = "2.2.0"
    const val mockwebserver = "3.7.0"
    const val firebase = "16.0.0"
    const val firebase_auth = "18.1.0"
    const val fcm = "17.0.0"
    const val crashlytics = "2.9.3"
    const val google_services_plugin = "3.1.1"
    const val fabric_plugin = "1.25.1"
    const val fastlane_screengrab = "1.2.0"
    const val jraska_falcon = "2.0.1"
    const val dagger = "2.16"
    const val coroutine = "1.2.0"
    const val play = "1.6.1"
}

object SystemEnv {
    val google_app_id: String? = System.getenv("google_app_id")
    val default_web_client_id: String? = System.getenv("default_web_client_id")
    val firebase_database_url: String? = System.getenv("firebase_database_url")
    val gcm_defaultSenderId: String? = System.getenv("gcm_defaultSenderId")
    val google_api_key: String? = System.getenv("google_api_key")
    val google_crash_reporting_api_key: String? = System.getenv("google_crash_reporting_api_key")
    val project_id: String? = System.getenv("project_id")
    val auto_screenshot: String? = System.getenv("auto_screenshot")
}

object Localization {
    val KEPT_LOCALE = arrayOf("in", "hi-rIN", "th", "tl", "su", "jv", "vi", "zh-rTW", "ta", "kn", "ml")
}
