import javafx.scene.image.Image


class Robot(x: Double, y: Double, id: Int, image: Image, d: Int) {
    val robotId: Int = id
    val robotImage: Image = image
    val delay: Int = d;
    var x: Double = x
        set(value) {
            field = value.also { require(it >= 0) { "Robot x must be non-negative" } }
        }
    var y: Double = y
        set(value) {
            field = value.also { require(it >= 0) { "Robot y must be non-negative" } }
        }

    init {
        require(id >= 0) { "Robot id must be non-negative" }
        require(delay >= 0) { "Robot delay must be non-negative" }
        require(x >= 0) { "Robot x must be non-negative" }
        require(y >= 0) { "Robot y must be non-negative" }
    }
}
