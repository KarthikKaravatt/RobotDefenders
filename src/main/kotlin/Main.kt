
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.stage.Stage

class HelloWorld : Application() {
    override fun start(primaryStage: Stage) {
        val label = Label("Hello World!")
        val root = StackPane()
        root.children.add(label)
        val scene = Scene(root, 300.0, 250.0)
        primaryStage.title = "Hello World!"
        primaryStage.scene = scene
        primaryStage.show()
    }
}

fun main() {
    Application.launch(HelloWorld::class.java)
}
