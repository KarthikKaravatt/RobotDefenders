import kotlin.math.pow
import kotlin.math.sqrt

data class Point(var x: Double, var y: Double)

//fun distance(p1: Point, p2: Point): Double {
//    return sqrt((p2.x - p1.x).pow(2.0) + (p2.y - p1.y).pow(2.0))
//}

fun vector(p1: Point, p2: Point): Point {
    return Point(p2.x - p1.x, p2.y - p1.y)
}


