package com.duoc.aquasample.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object NewSample : Screen("new_sample")
    data object History : Screen("history")
    data object Profile : Screen("profile")
    data object SampleForm : Screen("sample_form")
}