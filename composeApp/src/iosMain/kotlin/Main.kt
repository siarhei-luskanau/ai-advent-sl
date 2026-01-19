import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import template.KoinApp

fun mainViewController(): UIViewController =
    ComposeUIViewController {
        KoinApp()
    }
