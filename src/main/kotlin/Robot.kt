import javafx.application.Platform
import javafx.scene.image.Image
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean


class Robot(
    val robotID: Int,
    pos: Point,
    val robotImage: Image,
    private val delay: Int
) {
    @Volatile
    var pos: Point = pos
        private set

    @Volatile
    var futurePos: Point = pos
        private set

    init {
        require(robotID >= 0) { "Robot id must be non-negative" }
        require(delay >= 0) { "Robot delay must be non-negative" }
        require(pos.x >= 0) { "Robot x position must be non-negative" }
        require(pos.y >= 0) { "Robot y position must be non-negative" }
    }

    // TODO: Maybe get rid of the locks?
    private fun updatePos(newPos: Point) {
        synchronized(this) {
            pos = newPos
        }
    }

    private fun updateFuturePos(newFuturePos: Point) {
        synchronized(this) {
            futurePos = newFuturePos
        }
    }

    private fun isRobotAtPosition(x: Int, y: Int, robots: MutableMap<Int, Robot>): Boolean {
        return robots.values.any {
            it.pos.x == x.toDouble() && it.pos.y == y.toDouble() ||
                    it.futurePos.x == x.toDouble() && it.futurePos.y == y.toDouble()
        }
    }

    private fun isRobotAbleToMove(
        endX: Double,
        endY: Double,
        robots: MutableMap<Int, Robot>,
        arena: JFXArena
    ): Boolean {
        val centerPoint = Point(CENTER_X, CENTER_Y)
        var ableToMove = true
        if (endX == centerPoint.x && endY == centerPoint.y) {
            robots.remove(this.robotID)
            arena.setGameOver()
            ableToMove = false
        } else if (endX == centerPoint.x && this.pos.y == centerPoint.y) {
            robots.remove(this.robotID)
            arena.setGameOver()
            ableToMove = false
        } else if (endY == centerPoint.y && this.pos.x == centerPoint.x) {
            robots.remove(this.robotID)
            arena.setGameOver()
            ableToMove = false
        }
        return ableToMove
    }

    private fun moveRobot(arena: JFXArena, robots: MutableMap<Int, Robot>) {
        val centerPoint = Point(CENTER_X, CENTER_Y)
        val vector: Point = vector(this.pos, centerPoint)
        val xDirection: Int = if (vector.x > 0) MOVE_RIGHT else MOVE_LEFT
        val yDirection: Int = if (vector.y < 0) MOVE_DOWN else MOVE_UP
        val x: Double = this.pos.x + xDirection
        val y: Double = this.pos.y + yDirection
        val ableToMove = isRobotAbleToMove(x, y, robots, arena)
        if (ableToMove) {
            if (this.pos.x == centerPoint.x && !isRobotAtPosition(this.pos.x.toInt(), y.toInt(), robots)) {
                this.moveRobotPosition(this.pos.x, y, arena)
            } else if (this.pos.y == centerPoint.y && !isRobotAtPosition(x.toInt(), this.pos.y.toInt(), robots)) {
                this.moveRobotPosition(x, this.pos.y, arena)
            } else if (!isRobotAtPosition(this.pos.x.toInt(), y.toInt(), robots)) {
                this.moveRobotPosition(this.pos.x, y, arena)
            } else if (!isRobotAtPosition(x.toInt(), this.pos.y.toInt(), robots)) {
                this.moveRobotPosition(x, this.pos.y, arena)
            }
        }
    }

    private fun moveRobotPosition(x: Double, y: Double, arena: JFXArena) {
        // Does not allow the robot to move outside the grid
        val xPos = x.coerceIn(0.0, GRID_WIDTH - 1.0)
        val yPos = y.coerceIn(0.0, GRID_HEIGHT - 1.0)
        // Claims a position on the grid
        this.updateFuturePos(Point(xPos, yPos))
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < MOVEMENT_ANIMATION_DELAY) {
            // Calculates the progress of the animation, it always from the starting pos to the end pos
            val progress = (System.currentTimeMillis() - startTime).toDouble() / MOVEMENT_ANIMATION_DELAY
            val currentX = this.pos.x + (xPos - this.pos.x) * progress
            val currentY = this.pos.y + (yPos - this.pos.y) * progress
            this.updatePos(Point(currentX, currentY))
            //makes the animation smoother
            Thread.sleep(MOVEMENT_ANIMATION_INTERVALS)
            Platform.runLater {
                arena.requestLayout()
            }
        }
        this.updatePos(Point(xPos, yPos))
    }

    fun createRobotAiThread(
        robots: MutableMap<Int, Robot>,
        exec: ExecutorService,
        gameOver: AtomicBoolean,
        arena: JFXArena
    ) {
        val robotAiTask = Runnable {
            val centerPoint = Point(CENTER_X, CENTER_Y)
            while (this.pos != centerPoint && !gameOver.get()) {
                Thread.sleep(this.delay.toLong())
                if (robots.containsKey(this.robotID)) {
                    robots[this.robotID]?.moveRobot(arena, robots)
                } else {
                    Platform.runLater { arena.requestLayout() }
                }
            }
            exec.shutdown()
        }
        exec.execute(robotAiTask)

    }


}
