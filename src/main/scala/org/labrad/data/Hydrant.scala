package org.labrad.data

import java.util.Date
import org.labrad.types._
import scala.math
import scala.util.Random

// TODO: support generating random data from a pattern, not just a type
object Hydrant {

  private val random = new Random

  def randomType: Type = {
    def gen(nesting: Int, allowArray: Boolean = true): Type = {
      var options = Seq('_, 'b, 'i, 'w, 's, 't, 'v, 'c)
      if (nesting < 4) {
        if (allowArray) options ++= Seq('array, 'map)
        options ++= Seq('cluster, 'hmap)
      }
      val choice = options(random.nextInt(options.size))
      choice match {
        case '_ => TNone
        case 'b => TBool
        case 'i => TInt
        case 'w => TUInt
        case 's => TStr
        case 't => TTime
        case 'v => TValue(randomUnits)
        case 'c => TComplex(randomUnits)

        case 'cluster =>
          val size = random.nextInt(5) + 1
          val elems = Seq.fill(size) { gen(nesting + 1) }
          TCluster(elems: _*)

        case 'array =>
          val nDims = random.nextInt(3) + 1
          val elem = gen(nesting + nDims, allowArray = false)
          TArr(elem, nDims)

        case 'map =>
          val k = gen(nesting + 1, allowArray = false)
          val v = gen(nesting + 1, allowArray = true)
          TMap(k, v)

        case 'hmap =>
          val k = gen(nesting + 1, allowArray = false)
          val size = random.nextInt(8) + 2
          val values = Seq.fill(size) { gen(nesting + 1) }
          THMap(k, values: _*)

        case _ =>
          sys.error("should not happen")
      }
    }
    gen(0)
  }

  def randomUnits = {
    // TODO: we could be a bit more sophisticated here
    val choices = Seq("", "s", "ms", "us", "m", "m/s", "V^2/Hz", "V/Hz^1/2")
    choices(random.nextInt(choices.size))
  }

  def randomData: Data = randomData(randomType)
  def randomData(s: String): Data = randomData(Type(s))

  def randomData(t: Type): Data = t match {
    case TNone => randomNone
    case TBool => randomBool
    case TInt => randomInt
    case TUInt => randomUInt
    case TStr => randomStr
    case TTime => randomTime
    case TValue(units) => randomValue(units)
    case TComplex(units) => randomComplex(units)
    case t: TCluster => randomCluster(t)
    case t: TArr => randomArr(t)
    case t: TMap => randomMap(t)
    case t: THMap => randomHMap(t)
    case TError(t) => randomError(t)
  }

  def randomNone = Data.NONE

  def randomBool = Bool(random.nextBoolean)

  def randomInt = Integer(random.nextInt)

  def randomUInt = UInt(random.nextInt(2000000000).toLong)

  def randomStr = {
    val bytes = Array.ofDim[Byte](random.nextInt(100))
    random.nextBytes(bytes)
    Bytes(bytes)
  }

  def randomTime = {
    val time = System.currentTimeMillis + random.nextInt
    Time(new Date(time))
  }

  def randomValue(units: Option[String]) = units match {
    case None => Value(nextDouble, randomUnits)
    case Some(units) => Value(nextDouble, units)
  }

  def randomComplex(units: Option[String]) = units match {
    case None => Cplx(nextDouble, nextDouble)
    case Some(units) => Cplx(nextDouble, nextDouble, units)
  }

  private def nextDouble = random.nextGaussian * 1e9

  def randomCluster(t: TCluster) =
    Cluster(t.elems.map(randomData(_)): _*)

  def randomArr(t: TArr) = {
    val arr = Data(t)
    val emptyShape = Seq.tabulate(t.depth) { i => if (i < t.depth - 1) 1 else 0 }
    val shape = if (t.elem == TNone) {
      emptyShape
    } else {
      val shape = Seq.tabulate(t.depth) { i => random.nextInt(math.pow(2, 5 - t.depth).toInt) }
      val size = shape.product
      if (size == 0) emptyShape else shape
    }
    arr.setArrayShape(shape: _*)
    arr.flatIterator.foreach { elem =>
      elem.set(randomData(t.elem))
    }
    arr
  }

  def randomMap(t: TMap) = {
    val map = Data(t)
    val size = random.nextInt(math.pow(2, 5).toInt)
    map.setMapSize(size)
    for ((k, v) <- map.mapIterator) {
      k.set(randomData(t.key))
      v.set(randomData(t.value))
    }
    map
  }

  def randomHMap(t: THMap) = {
    val hmap = Data(t)
    for (((k, v), vType) <- hmap.mapIterator zip t.values.iterator) {
      k.set(randomData(t.key))
      v.set(randomData(vType))
    }
    hmap
  }

  def randomError(t: Type) = {
    val code = random.nextInt
    val message = "random error"
    val payload = randomData(t)
    Error(code, message, payload)
  }
}
