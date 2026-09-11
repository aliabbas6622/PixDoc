package com.example.core.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Browser : Screen("browser?path={path}") {
        fun createRoute(path: String? = null): String {
            return if (path != null) {
                "browser?path=${Uri.encode(path)}"
            } else {
                "browser"
            }
        }
    }

    object Viewer : Screen("viewer?path={path}") {
        fun createRoute(path: String): String {
            return "viewer?path=${Uri.encode(path)}"
        }
    }
}
