import javafx.scene.image.Image


class Robot(
    val robotID: Int,
    @Volatile override var pos: Point,
    override val image: Image,
    val delay: Int
) : GameObject() {

    @Volatile
    override var futurePos: Point = pos

    init {
        require(robotID >= 0) { "Robot id must be non-negative" }
        require(delay >= 0) { "Robot delay must be non-negative" }
        require(pos.x >= 0) { "Robot x position must be non-negative" }
        require(pos.y >= 0) { "Robot y position must be non-negative" }
    }

    // TODO: Maybe get rid of the locks?
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
