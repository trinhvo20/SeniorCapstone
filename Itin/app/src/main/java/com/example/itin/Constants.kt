package com.example.itin

class Constants {



    companion object {
        const val BASE_URL = "https://fcm.googleapis.com"
        // SERVER KEY IS A SECRET. SHOULD NOT BE FOUND IN CODE THAT ENDS UP IN GITHUB
        const val SERVER_KEY = BuildConfig.FCM_KEY
        const val CONTENT_TYPE = "application/json"
    }
}