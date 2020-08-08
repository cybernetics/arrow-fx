package arrow.benchmarks

import arrow.core.andThen
import arrow.fx.IO
import arrow.fx.coroutines.Platform
import arrow.fx.coroutines.trampoline
import cats.data.AndThen
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.CompilerControl
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit
import kotlin.coroutines.intrinsics.*

@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5)
@CompilerControl(CompilerControl.Mode.DONT_INLINE)
open class Map1 {

  @Benchmark
  fun zio(): Long =
    arrow.benchmarks.effects.scala.zio.`Map$`.`MODULE$`.zioMapTest(12000, 1)

  @Benchmark
  fun cats(): Long =
    arrow.benchmarks.effects.scala.cats.`Map$`.`MODULE$`.catsIOMapTest(12000, 1)

  @Benchmark
  fun legacy(): Long = ioTest(12000, 1)

  @Benchmark
  fun fx(): Long = env.unsafeRunSync { fxTest(12000, 1) }

  @Benchmark
  fun kio(): Long =
    arrow.benchmarks.effects.kio.Map.kioMapTest(12000, 1)

}

tailrec suspend fun fxTest(
  iterations: Int,
  batch: Int,
  j: Int = 0,
  i: Int = 0,
  f: (Int) -> Int = { x: Int -> x + 1 },
  io: suspend () -> Int = { 0 },
  sum: Long = 0
): Long =
  when {
    j < batch -> fxTest(
      iterations = iterations,
      batch = batch,
      j = j + 1,
      i = i,
      f = f,
      io = { f(io()) },
      sum = sum
    )
    i < iterations -> fxTest(
      iterations = iterations,
      batch = batch,
      j = j,
      i = i + 1,
      f = f,
      io = io,
      sum = sum + io()
    )
    else -> sum
  }


fun ioTest(iterations: Int, batch: Int): Long {
  val f = { x: Int -> x + 1 }
  var io = IO.just(0)

  var j = 0
  while (j < batch) {
    io = io.map(f); j += 1
  }

  var sum = 0L
  var i = 0
  while (i < iterations) {
    sum += io.unsafeRunSync()
    i += 1
  }
  return sum
}
