import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.TextArea
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import java.io.InputStream
import java.util.*

const val ROBOT_IMAGE_FILE: String = "1554047213.png"
const val CITADEL_IMAGE_FILE: String = "rg1024-isometric-tower.png"
const val WALL_IMAGE_FILE: String = "181478.png"
const val GRID_WIDTH: Int = 9
const val GRID_HEIGHT: Int = 9
const val CENTER_OFFSET: Double = 0.5
const val LABEL_OFFSET: Double = 1.0
const val ROBOT_SPAWN_RATE: Long = 1500
const val MIN_DELAY: Int = 500
const val MAX_DELAY: Int = 2000
const val MOVE_UP: Int = 1
const val MOVE_DOWN: Int = -1
const val MOVE_LEFT: Int = -1
const val MOVE_RIGHT: Int = 1
const val ROBOT_ID_CLAMP: Int = 10000
const val CENTER_X: Double = 4.0
const val CENTER_Y: Double = 4.0
const val MOVEMENT_ANIMATION_DELAY: Int = 400
const val MOVEMENT_ANIMATION_INTERVALS: Long = 10

class JFXArena(logger: TextArea, statusInfo: TextArea) : Pane() {
    private val gridWidth: Int = GRID_WIDTH
    private val gridHeight: Int = GRID_HEIGHT

    private var gridSquareSize: Double = 0.0
    private var canvas: Canvas = Canvas()
    private var listeners: MutableList<ArenaListener>? = null

    private val game = Game(this, logger, statusInfo)

    init {

        this.canvas.widthProperty().bind(widthProperty())
        this.canvas.heightProperty().bind(heightProperty())
        this.children.add(canvas)
        game.startGame()
    }

    private fun loadImage(file: String): InputStream {
        return javaClass.classLoader.getResourceAsStream(file)
            ?: throw AssertionError("Cannot find image file $file")
    }

    /**
     *
     */

    fun addListener(newListener: ArenaListener) {
        if (listeners == null) {
            listeners = LinkedList()
            setOnMouseClicked { event ->
                val gridX = (event.x / gridSquareSize).toInt()
                val gridY = (event.y / gridSquareSize).toInt()

                if (gridX < gridWidth && gridY < gridHeight) {
                    listeners?.let { nonNullListeners ->
                        for (listener in nonNullListeners) {
                            listener.squareClicked(gridX, gridY)
                        }
                    }
                }
            }
        }
        listeners?.add(newListener)
    }

    override fun layoutChildren() {
        super.layoutChildren()
        val gfx = canvas.graphicsContext2D
        gfx.clearRect(0.0, 0.0, canvas.width, canvas.height)

        gridSquareSize = minOf(
            width / gridWidth,
            height / gridHeight
        )

        val arenaPixelWidth = gridWidth * gridSquareSize
        val arenaPixelHeight = gridHeight * gridSquareSize
        val citadelImage = Image(loadImage(CITADEL_IMAGE_FILE))

        gfx.stroke = Color.DARKGREY
        gfx.strokeRect(0.0, 0.0, arenaPixelWidth - 1.0, arenaPixelHeight - 1.0)

        for (gridX in 1..<gridWidth) {
            val x = gridX * gridSquareSize
            gfx.strokeLine(x, 0.0, x, arenaPixelHeight)
        }

        for (gridY in 1..<gridHeight) {
            val y = gridY * gridSquareSize
            gfx.strokeLine(0.0, y, arenaPixelWidth, y)
        }

        // render robots and id
        for (robot in game.robotMap.values) {
            drawImage(gfx, robot.image, robot.pos.x, robot.pos.y)
            drawLabel(gfx, robot.robotID.toString(), robot.pos.x, robot.pos.y)
        }
        // render walls
        for (wall in game.walls.values) {
            drawImage(gfx, wall.image, wall.pos.x, wall.pos.y)
        }
        // render citadel
        drawImage(gfx, citadelImage, CENTER_X, CENTER_Y)
    }

    private fun drawImage(gfx: GraphicsContext, image: Image, gridX: Double, gridY: Double) {
        val x = (gridX + CENTER_OFFSET) * gridSquareSize
        val y = (gridY + CENTER_OFFSET) * gridSquareSize

        val fullSizePixelWidth = image.width
        val fullSizePixelHeight = image.height

        val displayedPixelWidth: Double
        val displayedPixelHeight: Double
        if (fullSizePixelWidth > fullSizePixelHeight) {
            displayedPixelWidth = gridSquareSize
            displayedPixelHeight = gridSquareSize * fullSizePixelHeight / fullSizePixelWidth
        } else {
            displayedPixelHeight = gridSquareSize
            displayedPixelWidth = gridSquareSize * fullSizePixelWidth / fullSizePixelHeight
        }

        gfx.drawImage(
            image,
            x - displayedPixelWidth / 2.0,
            y - displayedPixelHeight / 2.0,
            displayedPixelWidth,
            displayedPixelHeight
        )
    }

    private fun drawLabel(gfx: GraphicsContext, label: String, gridX: Double, gridY: Double) {
        gfx.textAlign = TextAlignment.CENTER
        gfx.textBaseline = VPos.TOP
        gfx.stroke = Color.BLUE
        gfx.strokeText(label, (gridX + CENTER_OFFSET) * gridSquareSize, (gridY + LABEL_OFFSET) * gridSquareSize)
    }

    fun stop() {
        game.endGame()
    }
}
