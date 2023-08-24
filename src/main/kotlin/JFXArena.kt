import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import java.io.InputStream
import java.lang.AssertionError
import java.util.*
import kotlin.random.Random

const val IMAGE_FILE: String = "1554047213.png"
const val GRID_WIDTH: Int = 9
const val GRID_HEIGHT: Int = 9
const val CENTER_OFFSET: Double = 0.5
const val ROBOT_SPAWN_RATE: Long = 1500
const val MIN_DELAY: Int = 500
const val MAX_DELAY: Int = 2000

class JFXArena : Pane() {
    private val gridWidth: Int = GRID_WIDTH
    private val gridHeight: Int = GRID_HEIGHT
    private var gridSquareSize: Double = 0.0
    private var canvas: Canvas = Canvas()

    private var robots: MutableMap<Int, Robot> = Collections.synchronizedMap(mutableMapOf<Int, Robot>())

    private var listeners: MutableList<ArenaListener>? = null

    init {

        this.canvas.widthProperty().bind(widthProperty())
        this.canvas.heightProperty().bind(heightProperty())
        this.children.add(canvas)
        createSpawnRobotThread()
    }

    private fun createSpawnRobotThread() {
        val spawnRobotThread = Thread {
            while (true) {
                Thread.sleep(ROBOT_SPAWN_RATE)
                spawnRobot()
            }
        }
        spawnRobotThread.start()
    }

    private fun spawnRobot() {
        var id = Random.nextInt(0, Int.MAX_VALUE)
        while (robots.containsKey(id)) {
            id = Random.nextInt(0, Int.MAX_VALUE)
        }
        val xPositions: List<Int> = listOf(0, GRID_WIDTH - 1)
        val yPositions: List<Int> = listOf(0, GRID_HEIGHT - 1)
        var x: Int = xPositions.random()
        var y: Int = yPositions.random()
        var delay: Int = Random.nextInt(MIN_DELAY, MAX_DELAY)
        while (robots.values.any { it.x == x.toDouble() && it.y == y.toDouble() }) {
            x = xPositions.random()
            y = yPositions.random()
            delay = Random.nextInt(MIN_DELAY, MAX_DELAY)
        }
        addRobot(id, x.toDouble(), y.toDouble(),delay)
    }


    private fun addRobot(id: Int, x: Double, y: Double, delay: Int) {
        val ioStream: InputStream = javaClass.classLoader.getResourceAsStream(IMAGE_FILE)
            ?: throw AssertionError("Cannot find image file $IMAGE_FILE")
        val robot = Robot(x, y, id, Image(ioStream), delay)
        robots[id] = robot
        requestLayout()
    }

    /**
     *
     */
    fun setRobotPosition(id: Int, x: Double, y: Double) {
        val robot: Robot? = robots[id]
        check(robot != null) { "Robot $id does not exist" }
        robot.x = x
        robot.y = y
        requestLayout()
    }

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

        for (robot in robots.values) {
            drawImage(gfx, robot.robotImage, robot.x, robot.y, robot.robotId)
        }
    }

    private fun drawImage(gfx: GraphicsContext, image: Image, gridX: Double, gridY: Double, id: Int) {
        val robot: Robot? = robots[id]
        check(robot != null) { "Robot $id does not exist" }
        val x = (gridX + CENTER_OFFSET) * gridSquareSize
        val y = (gridY + CENTER_OFFSET) * gridSquareSize

        val fullSizePixelWidth = robot.robotImage.width
        val fullSizePixelHeight = robot.robotImage.height

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

//    private fun drawLabel(gfx: GraphicsContext, label: String, gridX: Double, gridY: Double) {
//        gfx.textAlign = TextAlignment.CENTER
//        gfx.textBaseline = VPos.TOP
//        gfx.stroke = Color.BLUE
//        gfx.strokeText(label, (gridX + CENTER_OFFSET) * gridSquareSize, (gridY + LABEL_OFFSET) * gridSquareSize)
//    }

    //private fun drawLine(
    //    gfx: GraphicsContext,
    //    gridX1: Double,
    //    gridY1: Double,
    //    gridX2: Double,
    //    gridY2: Double
    //) {
    //    gfx.stroke = Color.RED

    //    val radius = LINE_RADIUS
    //    val angle = atan2(gridY2 - gridY1, gridX2 - gridX1)
    //    val clippedGridX1 = gridX1 + cos(angle) * radius
    //    val clippedGridY1 = gridY1 + sin(angle) * radius

    //    gfx.strokeLine(
    //        (clippedGridX1 + CENTER_OFFSET) * gridSquareSize,
    //        (clippedGridY1 + CENTER_OFFSET) * gridSquareSize,
    //        (gridX2 + CENTER_OFFSET) * gridSquareSize,
    //        (gridY2 + CENTER_OFFSET) * gridSquareSize
    //    )
    //}


}
