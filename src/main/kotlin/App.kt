import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.SplitPane
import javafx.scene.control.TextArea
import javafx.scene.control.ToolBar
import javafx.scene.layout.BorderPane
import javafx.stage.Stage

const val ARENA_WIDTH: Double = 300.0
const val SCENE_WIDTH: Double = 800.0
const val SCENE_HEIGHT: Double = 800.0

class App : Application() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(App::class.java)
        }
    }

    override fun start(stage: Stage) {
        stage.title = "Robot Defender"
        val logger = TextArea()
        val statusTextArea = TextArea()
        val arena = JFXArena(logger, statusTextArea)
        val toolbar = ToolBar()
        val label = Label("Score: 999")
        val splitPane = SplitPane()
        val contentPane = BorderPane()
        logger.isEditable = false
        toolbar.items.addAll(label)
        splitPane.items.addAll(arena, logger)
        arena.minWidth = ARENA_WIDTH
        contentPane.top = toolbar
        contentPane.center = splitPane
        statusTextArea.isEditable = false
        contentPane.bottom = statusTextArea

        val scene = Scene(contentPane, SCENE_WIDTH, SCENE_HEIGHT)
        stage.scene = scene
        stage.setOnCloseRequest {
            arena.stop()
        }
        stage.show()
    }
}
