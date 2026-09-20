import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import io.compositor.viewer.ui.CompositorApp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(
        title = "Compositor",
        canvasElementId = "ComposeTarget"
    ) {
        CompositorApp()
    }
}
