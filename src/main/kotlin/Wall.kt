import javafx.scene.image.Image
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

const val WALL_DAMAGED_IMAGE_FILE: String = "181479.png"
class Wall(
    val pos: Point,
    var image: Image
) {
    private var damaged: AtomicBoolean = AtomicBoolean(false)

    init {
        require(pos.x >= 0) { "Wall x position must be non-negative" }
        require(pos.y >= 0) { "Wall y position must be non-negative" }
    }

    fun setDamaged() {
        damaged.set(true)
        val stream: InputStream = javaClass.classLoader.getResourceAsStream(WALL_DAMAGED_IMAGE_FILE)
            ?: throw AssertionError("Cannot find image file $WALL_DAMAGED_IMAGE_FILE")
        image = Image(stream)
    }

    fun getDamaged(): Boolean {
        return damaged.get()
    }
}
