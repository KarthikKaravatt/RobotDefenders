import javafx.application.Platform
import javafx.scene.image.Image
import java.io.InputStream
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class Game(private val arena: JFXArena) {
    var robots: MutableMap<Int, Robot> = Collections.synchronizedMap(mutableMapOf<Int, Robot>())
    private var gameOver: AtomicBoolean = AtomicBoolean(false)
    private val executionService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    private fun addRobot(id: Int, x: Double, y: Double, delay: Int) {
        val ioStream: InputStream = javaClass.classLoader.getResourceAsStream(ROBOT_IMAGE_FILE)
            ?: throw AssertionError("Cannot find image file $ROBOT_IMAGE_FILE")
        val robot = Robot(id, Point(x, y), Image(ioStream), delay)

        robots[id] = robot
        Platform.runLater {
            arena.requestLayout()
        }
    }

    private fun isRobotAtPosition(x: Int, y: Int): Boolean {
        return robots.values.any {
            it.pos.x == x.toDouble() && it.pos.y == y.toDouble() ||
                    it.futurePos.x == x.toDouble() && it.futurePos.y == y.toDouble()
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
        if (!gameOver.get()) {
            robots[id]?.let { createRobotAiThread(it) }
        }
    }

    fun createSpawnRobotThread() {
        val spawnRobotThread = Thread {
            while (!gameOver.get()) {
                Thread.sleep(ROBOT_SPAWN_RATE)
                spawnRobot()
            }
        }
        // Stops thread when the main thread is stopped
        spawnRobotThread.isDaemon = true
        spawnRobotThread.start()
    }

    private fun isRobotAbleToMove(
        endX: Double,
        endY: Double,
        robot: Robot
    ): Boolean {
        val centerPoint = Point(CENTER_X, CENTER_Y)
        var ableToMove = true
        if (endX == centerPoint.x && endY == centerPoint.y) {
            robots.remove(robot.robotID)
            arena.setGameOver()
            ableToMove = false
        } else if (endX == centerPoint.x && robot.pos.y == centerPoint.y) {
            robots.remove(robot.robotID)
            arena.setGameOver()
            ableToMove = false
        } else if (endY == centerPoint.y && robot.pos.x == centerPoint.x) {
            robots.remove(robot.robotID)
            arena.setGameOver()
            ableToMove = false
        }
        return ableToMove
    }

    private fun moveRobotPosition(x: Double, y: Double, robot: Robot) {
        // Does not allow the robot to move outside the grid
        val xPos = x.coerceIn(0.0, GRID_WIDTH - 1.0)
        val yPos = y.coerceIn(0.0, GRID_HEIGHT - 1.0)
        // Claims a position on the grid
        robot.updateFuturePos(Point(xPos, yPos))
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < MOVEMENT_ANIMATION_DELAY) {
            // Calculates the progress of the animation, it always from the starting pos to the end pos
            val progress = (System.currentTimeMillis() - startTime).toDouble() / MOVEMENT_ANIMATION_DELAY
            val currentX = robot.pos.x + (xPos - robot.pos.x) * progress
            val currentY = robot.pos.y + (yPos - robot.pos.y) * progress
            robot.updatePos(Point(currentX, currentY))
            //makes the animation smoother
            Thread.sleep(MOVEMENT_ANIMATION_INTERVALS)
            Platform.runLater {
                arena.requestLayout()
            }
        }
        robot.updatePos(Point(xPos, yPos))
    }

    private fun moveRobot(robot: Robot) {
        val centerPoint = Point(CENTER_X, CENTER_Y)
        val vector: Point = vector(robot.pos, centerPoint)
        val xDirection: Int = if (vector.x > 0) MOVE_RIGHT else MOVE_LEFT
        val yDirection: Int = if (vector.y < 0) MOVE_DOWN else MOVE_UP
        val x: Double = robot.pos.x + xDirection
        val y: Double = robot.pos.y + yDirection
        val ableToMove = isRobotAbleToMove(x, y, robot)
        if (ableToMove) {
            if (robot.pos.x == centerPoint.x && !isRobotAtPosition(robot.pos.x.toInt(), y.toInt())) {
                moveRobotPosition(robot.pos.x, y, robot)
            } else if (robot.pos.y == centerPoint.y && !isRobotAtPosition(x.toInt(), robot.pos.y.toInt())) {
                moveRobotPosition(x, robot.pos.y, robot)
            } else if (!isRobotAtPosition(robot.pos.x.toInt(), y.toInt())) {
                moveRobotPosition(robot.pos.x, y, robot)
            } else if (!isRobotAtPosition(x.toInt(), robot.pos.y.toInt())) {
                moveRobotPosition(x, robot.pos.y, robot)
            }
        }
    }

    private fun createRobotAiThread(robot: Robot) {
        val robotAiTask = Runnable {
            val centerPoint = Point(CENTER_X, CENTER_Y)
            while (robot.pos != centerPoint && !gameOver.get()) {
                Thread.sleep(robot.delay.toLong())
                if (robots.containsKey(robot.robotID)) {
                    moveRobot(robot)
                } else {
                    Platform.runLater { arena.requestLayout() }
                }
            }
            executionService.shutdown()
        }
        executionService.execute(robotAiTask)

    }

    fun setGameOver() {
        gameOver.set(true)
    }
}
