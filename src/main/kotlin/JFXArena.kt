import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import java.lang.AssertionError
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

const val IMAGE_FILE: String = "1554047213.png"
const val GRID_WIDTH: Int = 9
const val GRID_HEIGHT: Int = 9
const val INITIAL_ROBOT_X: Double = 1.0
const val INITIAL_ROBOT_Y: Double = 3.0

class JFXArena : Pane() {
    private var robot: Image
    private val gridWidth: Int = GRID_WIDTH
    private val gridHeight: Int = GRID_HEIGHT
    private var robotX: Double = INITIAL_ROBOT_X
    private var robotY: Double = INITIAL_ROBOT_Y

    private var gridSquareSize: Double = 0.0
    private var canvas: Canvas = Canvas()

    private var listeners: MutableList<ArenaListener>? = null

    init {
        val ioStream = javaClass.classLoader.getResourceAsStream(IMAGE_FILE)
            ?: throw AssertionError("Cannot find image file $IMAGE_FILE")
        this.robot = Image(ioStream)

        this.canvas.widthProperty().bind(widthProperty())
        this.canvas.heightProperty().bind(heightProperty())
        this.children.add(canvas)
    }

    fun setRobotPosition(x: Double, y: Double){
        this.robotX = x
        this.robotY = y
        requestLayout()
    }
    fun addListener(newListener: ArenaListener) {
        if (listeners == null) {
            listeners = LinkedList()
            setOnMouseClicked { event ->
                val gridX = (event.x / gridSquareSize).toInt()
                val gridY = (event.y / gridSquareSize).toInt()

                if (gridX < gridWidth && gridY < gridHeight) {
                    listeners?.let {nonNullListeners ->
                       for(listener in nonNullListeners) {
                           listener.squareClicked(gridX,gridY)
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
            height / gridHeight)

        val arenaPixelWidth = gridWidth * gridSquareSize
        val arenaPixelHeight = gridHeight * gridSquareSize

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

        drawImage(gfx, robot, robotX, robotY)
        drawLabel(gfx, "Robot Name", robotX, robotY)
    }
    private fun drawImage(gfx: GraphicsContext, image: Image, gridX: Double, gridY: Double) {
        val x = (gridX + 0.5) * gridSquareSize
        val y = (gridY + 0.5) * gridSquareSize

        val fullSizePixelWidth = robot.width
        val fullSizePixelHeight = robot.height

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
        gfx.strokeText(label, (gridX + 0.5) * gridSquareSize, (gridY + 1.0) * gridSquareSize)
    }

    private fun drawLine(
        gfx: GraphicsContext,
        gridX1: Double,
        gridY1: Double,
        gridX2: Double,
        gridY2: Double
    ) {
        gfx.stroke = Color.RED

        val radius = 0.5
        val angle = atan2(gridY2 - gridY1, gridX2 - gridX1)
        val clippedGridX1 = gridX1 + cos(angle) * radius
        val clippedGridY1 = gridY1 + sin(angle) * radius

        gfx.strokeLine(
            (clippedGridX1 + 0.5) * gridSquareSize,
            (clippedGridY1 + 0.5) * gridSquareSize,
            (gridX2 + 0.5) * gridSquareSize,
            (gridY2 + 0.5) * gridSquareSize
        )
    }


}
