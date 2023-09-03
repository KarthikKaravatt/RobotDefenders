import javafx.scene.image.Image

class Robot(
    val robotID: Int,
    @Volatile var pos: Point,
    val image: Image,
    val delay: Int
) {

    @Volatile
    var futurePos: Point = pos

    init {
        require(robotID >= 0) { "Robot id must be non-negative" }
        require(delay >= 0) { "Robot delay must be non-negative" }
        require(pos.x >= 0) { "Robot x position must be non-negative" }
        require(pos.y >= 0) { "Robot y position must be non-negative" }
    }

    fun updatePos(newPos: Point) {
        // position can be changed by JFX thread or robot thread
        synchronized(this) {
            pos = newPos
        }
    }

    fun updateFuturePos(newFuturePos: Point) {
        // position can be changed by JFX thread or robot thread
        synchronized(this) {
            futurePos = newFuturePos
        }
    }
}
