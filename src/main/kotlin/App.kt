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
class App: Application() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(App::class.java)
        }
    }

    override fun start(stage: Stage) {
        stage.title = "Example App (JavaFX)"
        val arena = JFXArena()
        val listener = object : ArenaListener {
            override fun squareClicked(x: Int, y: Int) {
                arena.setRobotPosition(0.0,0.0)
            }
        }
        arena.addListener(listener)
        val toolbar = ToolBar()

        val label = Label("Score: 999")

        toolbar.items.addAll(label)

        val logger = TextArea()
        logger.appendText("Hello\n")
        logger.appendText("World\n")

        val splitPane = SplitPane()
        splitPane.items.addAll(arena, logger)
        arena.minWidth =ARENA_WIDTH

        val contentPane = BorderPane()
        contentPane.top = toolbar
        contentPane.center = splitPane

        val scene = Scene(contentPane, SCENE_WIDTH, SCENE_HEIGHT)
        stage.scene = scene
        stage.show()
    }
}

