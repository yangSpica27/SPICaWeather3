package me.spica.spicaweather3.utils


class RainDropManager {


  val rainDrops = mutableListOf<RainDrop>()


  fun init(
    width: Int,
    height: Int
  ) {
    synchronized(this) {
      rainDrops.clear()
      for (i in 0..100) {
        rainDrops.add(
          RainDrop(
            randomInstance.nextInt(width).toDouble(),
            -randomInstance.nextInt(height).toDouble(),
            width.toDouble(),
            height.toDouble()
          )
        )
      }
    }
  }

  fun update() {
    synchronized(this) {
      rainDrops.forEach {
        it.next()
      }
    }
  }

}

private val randomInstance = kotlin.random.Random

data class RainDrop(
  val initX: Double,
  val initY: Double,
  val maxDx: Double,
  val maxDy: Double,
) {

  var dx: Double
  var dy: Double
  val speed: Double
  val angle: Double

  init {
    val random: Double = 0.4 + 0.12 * randomInstance.nextDouble() * 5
    dx = initX
    dy = initY
    speed =10 * random
    angle = 7.5 - (dx / maxDx) * 15
  }

  fun next() {

    dy = if (dy < maxDy * 1.2) {
      dy + speed / 2 + speed / 2 * (maxDy - dy) / maxDy
    } else {
      initY
    }
  }
}
