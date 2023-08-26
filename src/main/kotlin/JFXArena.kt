import javafx.application.Platform
import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import java.io.InputStream
import java.lang.AssertionError
import java.util.*
import java.util.concurrent.Executors
import kotlin.random.Random

const val ROBOT_IMAGE_FILE: String = "1554047213.png"
const val CITADEL_IMAGE_FILE: String = "rg1024-isometric-tower.png"
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
const val CENTER_X: Double = (GRID_WIDTH - 1).div(2.0)
const val CENTER_Y: Double = (GRID_HEIGHT - 1).div(2.0)

class JFXArena : Pane() {
    private val gridWidth: Int = GRID_WIDTH
    private val gridHeight: Int = GRID_HEIGHT
    private var gridSquareSize: Double = 0.0
    private var canvas: Canvas = Canvas()

    private var robots: MutableMap<Int, Robot> = Collections.synchronizedMap(mutableMapOf<Int, Robot>())
//    private var mapMatrix: MutableList<MutableList<Int>> = MutableList(GRID_WIDTH) { MutableList(GRID_HEIGHT) { -1 } }

    private var listeners: MutableList<ArenaListener>? = null

    private val executionService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())


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
        // Stops thread when the main thread is stopped
        spawnRobotThread.isDaemon = true
        spawnRobotThread.start()
    }

    private fun createRobotAiThread(id: Int) {
        val robot: Robot? = robots[id]
        check(robot != null) { "Robot $id does not exist" }
        val robotAiTask = Runnable {
            val centerPoint = Point(CENTER_X, CENTER_Y)
            while (robot.pos != centerPoint) {
                Thread.sleep(robot.delay.toLong())
                if (robots.containsKey(id)) {
                    moveRobot(id)
                }
            }
        }
        executionService.execute(robotAiTask)

    }

    private fun moveRobot(id: Int) {
        val robot: Robot? = robots[id]
        check(robot != null) { "Robot $id does not exist" }
        val centerPoint = Point(CENTER_X, CENTER_Y)
        val vector: Point = vector(robot.pos, centerPoint)
        val xDirection: Int = if (vector.x > 0) MOVE_RIGHT else MOVE_LEFT
        val yDirection: Int = if (vector.y < 0) MOVE_DOWN else MOVE_UP
        val x: Double = robot.pos.x + xDirection
        val y: Double = robot.pos.y + yDirection
        val moves = mapOf("Horizontal" to x, "Vertical" to y)
        val move = moves.keys.random()

        if (x == centerPoint.x && y == centerPoint.y) {
            robots.remove(id)
            return
        }
        if (isRobotAtPosition(x.toInt(), y.toInt())) {
            return
        }
        if (move == "Horizontal") {
            moveRobotPosition(id, x, robot.pos.y)
        } else {
            moveRobotPosition(id, robot.pos.x, y)
        }
    }

    private fun spawnRobot() {
        var id = Random.nextInt(0, Int.MAX_VALUE) % ROBOT_ID_CLAMP
        while (robots.containsKey(id)) {
            id = Random.nextInt(0, Int.MAX_VALUE) % ROBOT_ID_CLAMP
        }
        val xPositions: List<Int> = listOf(0, GRID_WIDTH - 1)
        val yPositions: List<Int> = listOf(0, GRID_HEIGHT - 1)
        var x: Int = xPositions.random()
        var y: Int = yPositions.random()
        val delay: Int = Random.nextInt(MIN_DELAY, MAX_DELAY)
        while (isRobotAtPosition(x, y)) {
            x = xPositions.random()
            y = yPositions.random()
        }
        addRobot(id, x.toDouble(), y.toDouble(), delay)
        createRobotAiThread(id)
    }

    private fun isRobotAtPosition(x: Int, y: Int): Boolean {
        return robots.values.any {
            it.pos.x == x.toDouble() && it.pos.y == y.toDouble() ||
                    it.futurePos.x == x.toDouble() && it.futurePos.y == y.toDouble()
        }
    }

    private fun loadCitadelImage(): InputStream {
        return javaClass.classLoader.getResourceAsStream(CITADEL_IMAGE_FILE)
            ?: throw AssertionError("Cannot find image file $CITADEL_IMAGE_FILE")
    }

    private fun addRobot(id: Int, x: Double, y: Double, delay: Int) {
        val ioStream: InputStream = javaClass.classLoader.getResourceAsStream(ROBOT_IMAGE_FILE)
            ?: throw AssertionError("Cannot find image file $ROBOT_IMAGE_FILE")
        val robot = Robot(id, Point(x, y), Image(ioStream), delay)
        robots[id] = robot
        Platform.runLater {
            requestLayout()
        }
    }

    /**
     *
     */
    private fun moveRobotPosition(id: Int, x: Double, y: Double) {
        val robot: Robot? = robots[id]
        check(robot != null) { "Robot $id does not exist" }
        val xPos = x.coerceIn(0.0, GRID_WIDTH - 1.0)
        val yPos = y.coerceIn(0.0, GRID_HEIGHT - 1.0)
        robot.updateFuturePos(Point(xPos, yPos))
        Thread.sleep(robot.delay.toLong())
        robot.updatePos(Point(xPos, yPos))
        Platform.runLater {
            requestLayout()
        }
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
        val citadelImage = Image(loadCitadelImage())

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
            drawImage(gfx, robot.robotImage, robot.pos.x, robot.pos.y)
            drawLabel(gfx, robot.robotID.toString(), robot.pos.x, robot.pos.y)
        }
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
