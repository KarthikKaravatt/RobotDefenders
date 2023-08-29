import javafx.scene.image.Image
import java.util.concurrent.atomic.AtomicBoolean

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
    }

    fun getDamaged(): Boolean {
        return damaged.get()
    }

}
