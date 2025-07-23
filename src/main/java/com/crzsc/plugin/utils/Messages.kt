package com.crzsc.plugin.utils

import java.util.*


fun message(key: String): String {
    val l = Locale.getDefault().language
    return try {
        if (l == "zh") {
            val local = Locale("zh", "CN")
            ResourceBundle.getBundle("messages.MessagesBundle", local).getString(key)
        } else {
            ResourceBundle.getBundle("messages.MessagesBundle").getString(key)
        }
    } catch (e: Exception) {
        ResourceBundle.getBundle("messages.MessagesBundle").getString(key)
    }
}

