package net.nastich.factory

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import math.BigDecimal.RoundingMode
import scala.concurrent.duration._

/**
 * Factory settings, contains parameters of manufacturing and assembly times, prices and throughputs.
 *
 * @author sena0713
 * @since 20.09.2014 
 */
case class Settings(config: Config) {

  import config._

  private val workerPrefix = "net.nastich.factory.worker"
  private val assemblyPrefix = "net.nastich.factory.assembly"

  /** The time that's needed to produce any part (fixed). */
  val ManufacturingTime = getMillis(s"$workerPrefix.manufacturing-time")
  /** The price one has to pay a Master for a Part. The price for a table leg is double this value. */
  val PartBasePrice = getPrice(s"$workerPrefix.base-price")

  /** The time that's needed to produce an Item from parts. */
  val AssemblyDuration = getMillis(s"$assemblyPrefix.assembly-time")
  /** Amount of items that can be produced simultaneously */
  val AssemblyCapacity = getInt(s"$assemblyPrefix.nr-of-instances")
  /** Fixed cost of item assembly. */
  val AssemblyPrice = BigDecimal(getString(s"$assemblyPrefix.price"))

  private def getMillis(path: String): FiniteDuration = getDuration(path, MILLISECONDS).millis
  private def getPrice(path: String): BigDecimal = BigDecimal(getDouble(path)).setScale(2, RoundingMode.HALF_UP)

}