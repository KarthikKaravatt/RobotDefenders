import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TextArea
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
const val SCORE_PER_SECOND: Int = 10
const val SECOND_MILI: Long = 1000
const val SCORE_ROBOT_DESTROYED: Int = 100

// sorry, but the game class will be a bit long because it has to handle a lot of things
// Could split it up into different classes, but I think it would be more confusing
@Suppress("TooManyFunctions")
class Game(private val arena: JFXArena, private val logger: TextArea, statusInfo: TextArea) {
    var walls: MutableMap<Point, Wall> = Collections.synchronizedMap(mutableMapOf<Point, Wall>())
        private set
    var robotMap: MutableMap<Int, Robot> = Collections.synchronizedMap(mutableMapOf<Int, Robot>())
        private set
    private var wallQueue: BlockingQueue<Point> = ArrayBlockingQueue(WALL_MAX_CAPACITY)
    private var gameOver: AtomicBoolean = AtomicBoolean(false)
    private val executionService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private val wallPlaceLock = ReentrantLock()
    private var score: Int = 0
    private var wallAmount: Int = 0

    // number of queued-up walls
    private val wallAmountProperty = SimpleStringProperty("Walls: $wallAmount/$WALL_MAX_CAPACITY\n")
    private val scoreProperty = SimpleStringProperty("Score: ${score}\n")
    private val statusTextBinding = Bindings.concat(wallAmountProperty, scoreProperty)
    private val scoreLock = ReentrantLock()
    private val wallStatusLock = ReentrantLock()
    private lateinit var spawnRobotThread: Thread
    private lateinit var wallThread: Thread
    private lateinit var scoreThread: Thread

    init {
        statusInfo.textProperty().bind(statusTextBinding)
    }

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
            it.pos.x == x.toDouble() &&
                it.pos.y == y.toDouble() ||
                it.futurePos.x == x.toDouble() &&
                it.futurePos.y == y.toDouble()
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
        logger.appendText("Robot $id spawned\n")
        Platform.runLater { arena.requestLayout() }
        // Start the robot AI thread if the game is not over
        if (!gameOver.get()) {
            robotMap[id]?.let { createRobotAiThread(it) }
        }
    }

    private fun incrementWallAmount() {
        wallStatusLock.lock()
        try {
            wallAmount += 1
            wallAmountProperty.set("Walls: $wallAmount/$WALL_MAX_CAPACITY\n")
        } finally {
            wallStatusLock.unlock()
        }
    }

    private fun decrementWallAmount() {
        wallStatusLock.lock()
        try {
            wallAmount -= 1
            wallAmountProperty.set("Walls: $wallAmount/$WALL_MAX_CAPACITY\n")
        } finally {
            wallStatusLock.unlock()
        }
    }

    private fun addScore(score: Int) {
        scoreLock.lock()
        try {
            this.score += score
            scoreProperty.set("Score: ${this.score}\n")
        } finally {
            scoreLock.unlock()
        }
    }

    private fun isRobotAboutToWin(
        endX: Double,
        endY: Double,
        robot: Robot
    ): Boolean {
        val centerPoint = Point(CENTER_X, CENTER_Y)
        val isMovingHorizontallyThroughCenter = endX == centerPoint.x && robot.pos.y == centerPoint.y
        val isMovingVerticallyThroughCenter = endY == centerPoint.y && robot.pos.x == centerPoint.x
        if (isMovingHorizontallyThroughCenter || isMovingVerticallyThroughCenter) {
            moveRobotPosition(CENTER_X, CENTER_Y, robot)
            robotMap.remove(robot.robotID)
            Platform.runLater { arena.requestLayout() }
            setGameOver()
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
            // makes the animation smoother
            Thread.sleep(MOVEMENT_ANIMATION_INTERVALS)
            Platform.runLater {
                arena.requestLayout()
            }
        }
        robot.updatePos(Point(xPos, yPos))
        Platform.runLater { arena.requestLayout() }
    }

    private fun handleWallCollision(x: Double, y: Double, robot: Robot) {
        if (walls.containsKey(Point(x, y))) {
            walls[Point(x, y)]?.let { wall ->
                if (wall.getDamaged()) {
                    walls.remove(Point(x, y))
                    decrementWallAmount()
                    logger.appendText("Wall at ${x.toInt()}, ${y.toInt()} destroyed\n")
                } else {
                    wall.setDamaged()
                    logger.appendText("Wall at ${x.toInt()}, ${y.toInt()} damaged\n")
                }
                moveRobotPosition(x, y, robot)
                robotMap.remove(robot.robotID)
                addScore(SCORE_ROBOT_DESTROYED)
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
            // thread will stop if the robot is removed
            while (robot.pos != centerPoint && !gameOver.get() && robotMap.containsKey(robot.robotID)) {
                Thread.sleep(robot.delay.toLong())
                moveRobot(robot)
            }
            Platform.runLater { arena.requestLayout() }
        }
        executionService.execute(robotAiTask)
    }

    private fun setGameOver() {
        gameOver.set(true)
        logger.appendText("Game Over\n")
        Platform.runLater { arena.requestLayout() }
        executionService.shutdown()
    }

    private fun startRobotSpawnThread() {
        // spawn the robots
        spawnRobotThread = Thread {
            while (!gameOver.get()) {
                try {
                    Thread.sleep(ROBOT_SPAWN_RATE)
                } catch (e: InterruptedException) {
                    println("Spawn robot thread interrupted")
                }
                spawnRobot()
            }
        }
        // Stops thread when the main thread is stopped
        spawnRobotThread.isDaemon = true
        spawnRobotThread.start()
    }

    private fun startWallThread() {
        wallThread = Thread {
            var addDelay: Boolean
            while (!gameOver.get()) {
                try {
                    val ioStream: InputStream = javaClass.classLoader.getResourceAsStream(WALL_IMAGE_FILE)
                        ?: throw AssertionError("Cannot find image file $WALL_IMAGE_FILE")
                    // take the clicked point from the javaFX thread
                    val wallPos = wallQueue.take()
                    val wall = Wall(wallPos, Image(ioStream))
                    if (walls.containsKey(wallPos) || isRobotAtPosition(wallPos.x.toInt(), wallPos.y.toInt())) {
                        decrementWallAmount()
                        addDelay = false
                    } else {
                        walls[wallPos] = wall
                        logger.appendText("Wall placed at ${wallPos.x.toInt()}, ${wallPos.y.toInt()}\n")
                        Platform.runLater { arena.requestLayout() }
                        addDelay = true
                    }
                    if (addDelay) {
                        try {
                            Thread.sleep(WALL_PLACE_DELAY)
                        } catch (e: InterruptedException) {
                            println("Wall thread interrupted")
                        }
                    }
                } catch (e: InterruptedException) {
                    println("Wall thread interrupted")
                }
            }
        }
        wallThread.isDaemon = true
        wallThread.start()
    }

    private fun startScoreThread() {
        scoreThread = Thread {
            while (!gameOver.get()) {
                // Increment the score by 10 points every second
                // and update the scoreProperty to be atomic
                addScore(SCORE_PER_SECOND)
                try {
                    Thread.sleep(SECOND_MILI)
                } catch (e: InterruptedException) {
                    println("Score thread interrupted")
                }
            }
        }
        scoreThread.isDaemon = true
        scoreThread.start()
    }

    private fun addClickListener() {
        val listener = object : ArenaListener {
            override fun squareClicked(x: Int, y: Int) {
                wallPlaceLock.lock()
                try {
                    if (wallAmount >= WALL_MAX_CAPACITY) {
                        return
                    }
                } finally {
                    wallPlaceLock.unlock()
                }
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
                    if (walls.size < WALL_MAX_CAPACITY) {
                        // pass the clicked point to the wall thread
                        wallQueue.put(clickedPoint)
                        incrementWallAmount()
                    }
                }
            }
        }
        arena.addListener(listener)
    }

    fun startGame() {
        addClickListener()
        startRobotSpawnThread()
        startWallThread()
        startScoreThread()
    }

    fun endGame() {
        gameOver.set(true)
        if (spawnRobotThread.isAlive) {
            spawnRobotThread.interrupt()
        }
        if (wallThread.isAlive) {
            wallThread.interrupt()
        }
        if (scoreThread.isAlive) {
            scoreThread.interrupt()
        }
        if (!executionService.isShutdown) {
            executionService.shutdown()
        }
    }
}
