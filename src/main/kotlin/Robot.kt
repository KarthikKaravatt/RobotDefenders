import javafx.scene.image.Image


class Robot(
    val robotID: Int,
    pos: Point,
    val robotImage: Image,
    val delay: Int
) {
    @Volatile var pos: Point = pos
        private set
    @Volatile var futurePos: Point = pos
        private set

    init {
        require(robotID >= 0) { "Robot id must be non-negative" }
        require(delay >= 0) { "Robot delay must be non-negative" }
        require(pos.x >= 0) { "Robot x position must be non-negative" }
        require(pos.y >= 0) { "Robot y position must be non-negative" }
    }

    fun updatePos(newPos: Point) {
        synchronized(this) {
            pos = newPos
        }
    }

    fun updateFuturePos(newFuturePos: Point) {
        synchronized(this) {
            futurePos = newFuturePos
        }
    }
}
