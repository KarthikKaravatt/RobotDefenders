import javafx.application.Platform
import javafx.scene.image.Image
import java.io.InputStream
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.random.Random

const val WALL_PLACE_DELAY: Long = 2000
const val WALL_MAX_CAPACITY: Int = 10

class Game(private val arena: JFXArena) {
    var walls: MutableMap<Point, Wall> = Collections.synchronizedMap(mutableMapOf<Point, Wall>())
        private set
    var robotMap: MutableMap<Int, Robot> = Collections.synchronizedMap(mutableMapOf<Int, Robot>())
        private set
    private var wallQueue: BlockingQueue<Point> = ArrayBlockingQueue(WALL_MAX_CAPACITY)
    private var gameOver: AtomicBoolean = AtomicBoolean(false)
    private val executionService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private val wallPlaceLock = ReentrantLock()


    private fun addRobot(id: Int, x: Double, y: Double, delay: Int) {
        val ioStream: InputStream = javaClass.classLoader.getResourceAsStream(ROBOT_IMAGE_FILE)
            ?: throw AssertionError("Cannot find image file $ROBOT_IMAGE_FILE")
        val robot = Robot(id, Point(x, y), Image(ioStream), delay)
        robotMap[id] = robot
        Platform.runLater {
            arena.requestLayout()
        }
    }

    private fun isRobotAtPosition(x: Int, y: Int): Boolean {
        return robotMap.values.any {
            it.pos.x == x.toDouble() && it.pos.y == y.toDouble() ||
                    it.futurePos.x == x.toDouble() && it.futurePos.y == y.toDouble()
        }
    }


    private fun spawnRobot() {
        // Generate a unique id for the robot
        var id = Random.nextInt(0, Int.MAX_VALUE) % ROBOT_ID_CLAMP
        while (robotMap.containsKey(id)) {
            id = Random.nextInt(0, Int.MAX_VALUE) % ROBOT_ID_CLAMP
        }
        // Choose a random position at the edge of the grid
        val xPositions: List<Int> = listOf(0, GRID_WIDTH - 1)
        val yPositions: List<Int> = listOf(0, GRID_HEIGHT - 1)
        var x: Int
        var y: Int
        do {
            x = xPositions.random()
            y = yPositions.random()
        } while (isRobotAtPosition(x, y) || walls.containsKey(Point(x.toDouble(), y.toDouble())))
        // Create a robot with a random delay
        val delay: Int = Random.nextInt(MIN_DELAY, MAX_DELAY)
        addRobot(id, x.toDouble(), y.toDouble(), delay)
        // Start the robot AI thread if the game is not over
        if (!gameOver.get()) {
            robotMap[id]?.let { createRobotAiThread(it) }
            println("robot $id spawned")
        }
    }
    fun startGame() {
        val listener = object : ArenaListener {
            override fun squareClicked(x: Int, y: Int) {
                val clickedPoint = Point(x.toDouble(), y.toDouble())
                // Cannot place wall on spawn points or center point
                val illegalPoints = setOf(
                    Point(CENTER_X, CENTER_Y),
                    Point(GRID_WIDTH.toDouble() - 1, GRID_HEIGHT.toDouble() - 1),
                    Point(0.0, 0.0),
                    Point(GRID_WIDTH.toDouble() - 1, 0.0),
                    Point(0.0, GRID_HEIGHT.toDouble() - 1)
                )
                if (clickedPoint !in illegalPoints) {
                    wallPlaceLock.lock()
                    try {
                        if (walls.size < WALL_MAX_CAPACITY) {
                            // pass the clicked point to the wall thread
                            wallQueue.put(clickedPoint)
                        }
                    } finally {
                        wallPlaceLock.unlock()
                    }
                }
            }
        }
        arena.addListener(listener)
        // spawn the robots
        val spawnRobotThread = Thread {
            while (!gameOver.get()) {
                Thread.sleep(ROBOT_SPAWN_RATE)
                spawnRobot()
            }
        }
        // Stops thread when the main thread is stopped
        spawnRobotThread.isDaemon = true
        spawnRobotThread.start()
        val wallThread = Thread {
            var addDelay: Boolean
            while (!gameOver.get()) {
                val ioStream: InputStream = javaClass.classLoader.getResourceAsStream(WALL_IMAGE_FILE)
                    ?: throw AssertionError("Cannot find image file $WALL_IMAGE_FILE")
                // take the clicked point from the javaFX thread
                val wallPos = wallQueue.take()
                val wall = Wall(wallPos, Image(ioStream))
                if (walls.containsKey(wallPos) || isRobotAtPosition(wallPos.x.toInt(), wallPos.y.toInt())) {
                    addDelay = false
                } else {
                    walls[wallPos] = wall
                    Platform.runLater {
                        arena.requestLayout()
                    }
                    addDelay = true
                }
                if (addDelay) {
                    Thread.sleep(WALL_PLACE_DELAY)
                }
            }
        }
        wallThread.isDaemon = true
        wallThread.start()
    }

    private fun isRobotAboutToWin(
        endX: Double,
        endY: Double,
        robot: Robot
    ): Boolean {
        val centerPoint = Point(CENTER_X, CENTER_Y)
        val isMovingToCenter = endX == centerPoint.x && endY == centerPoint.y
        val isMovingHorizontallyThroughCenter = endX == centerPoint.x && robot.pos.y == centerPoint.y
        val isMovingVerticallyThroughCenter = endY == centerPoint.y && robot.pos.x == centerPoint.x
        if (isMovingToCenter || isMovingHorizontallyThroughCenter || isMovingVerticallyThroughCenter) {
            robotMap.remove(robot.robotID)
            arena.setGameOver()
            return false
        }
        return true
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

    private fun handleWallCollision(x: Double, y: Double, robot: Robot) {
        if (walls.containsKey(Point(x, y))) {
            walls[Point(x, y)]?.let { wall ->
                if (wall.getDamaged()) {
                    walls.remove(Point(x, y))
                } else {
                    wall.setDamaged()
                }
                robotMap.remove(robot.robotID)
                Platform.runLater { arena.requestLayout() }
            }
        } else {
            moveRobotPosition(x, y, robot)
        }
    }

    private fun moveRobot(robot: Robot) {
        val centerPoint = Point(CENTER_X, CENTER_Y)
        val vector: Point = vector(robot.pos, centerPoint)
        val xDirection: Int = if (vector.x > 0) MOVE_RIGHT else MOVE_LEFT
        val yDirection: Int = if (vector.y < 0) MOVE_DOWN else MOVE_UP
        val x: Double = robot.pos.x + xDirection
        val y: Double = robot.pos.y + yDirection
        if (isRobotAboutToWin(x, y, robot)) {
            when {
                robot.pos.x == centerPoint.x && !isRobotAtPosition(
                    robot.pos.x.toInt(),
                    y.toInt()
                ) -> handleWallCollision(robot.pos.x, y, robot)

                robot.pos.y == centerPoint.y && !isRobotAtPosition(
                    x.toInt(),
                    robot.pos.y.toInt()
                ) -> handleWallCollision(x, robot.pos.y, robot)

                !isRobotAtPosition(robot.pos.x.toInt(), y.toInt()) -> handleWallCollision(robot.pos.x, y, robot)
                !isRobotAtPosition(x.toInt(), robot.pos.y.toInt()) -> handleWallCollision(x, robot.pos.y, robot)
            }
        }
    }

    private fun createRobotAiThread(robot: Robot) {
        val robotAiTask = Runnable {
            val centerPoint = Point(CENTER_X, CENTER_Y)
            while (robot.pos != centerPoint && !gameOver.get()) {
                Thread.sleep(robot.delay.toLong())
                if (robotMap.containsKey(robot.robotID)) {
                    moveRobot(robot)
                } else {
                    Platform.runLater { arena.requestLayout() }
                    break
                }
            }
        }
        executionService.execute(robotAiTask)

    }

    fun setGameOver() {
        gameOver.set(true)
        executionService.shutdown()
    }

}
