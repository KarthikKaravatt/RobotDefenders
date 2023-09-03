data class Point(var x: Double, var y: Double)
fun vector(p1: Point, p2: Point): Point {
    return Point(p2.x - p1.x, p2.y - p1.y)
}
