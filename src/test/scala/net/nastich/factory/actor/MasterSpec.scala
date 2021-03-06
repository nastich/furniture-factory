package net.nastich.factory.actor

import net.nastich.factory.actor.Manufacturer._
import net.nastich.factory.actor.Master._
import net.nastich.factory.common.TestKitWordSpec
import net.nastich.factory.model._
import org.scalatest.Assertions

import scala.concurrent.blocking
import scala.concurrent.duration._

class MasterSpec extends TestKitWordSpec("MasterSpec") with Assertions {

  val defaultPrice = BigDecimal(10)

  "A Master" should {
    "reject parts that it doesn't know" in {
      val master = system.actorOf(Master.props(classOf[ChairTop], defaultPrice, 1.milli), "partRejector")

      master ! PartRequest(1L, classOf[TableTop])
      expectMsg(UnrecognizedPartType(1L, classOf[ChairTop]))
    }

    "accept parts that are subclasses of what's configured" in {
      val master = system.actorOf(Master.props(classOf[ChairTop], defaultPrice, 1.milli), "hierarchyAcceptor")

      master ! PartRequest(1L, classOf[ChairSeat])
      expectMsgPF() { case PartComplete(1L, `defaultPrice`, ChairSeat(_)) => }

      master ! PartRequest(2L, classOf[ChairBack])
      expectMsgPF() { case PartComplete(2L, `defaultPrice`, ChairBack(_)) => }
    }

    "take time to produce parts" in {
      val duration = 50.millis
      val master = system.actorOf(Master.props(classOf[Leg], defaultPrice, duration), "longTimeWorker")
      master ! PartRequest(1L, classOf[ChairLeg])
      expectNoMsg(duration mul 4 div 5)
      expectMsgPF() { case PartComplete(1L, `defaultPrice`, ChairLeg(_)) => () }
    }

    "produce one part at a time" in {
      val duration = 100.millis
      val master = system.actorOf(Master.props(classOf[ChairTop], defaultPrice, duration), "onePartProducer")

      master ! PartRequest(1L, classOf[ChairBack])
      master ! PartRequest(2L, classOf[ChairBack])
      within(duration * 2) {
        expectMsgPF() { case PartComplete(1L, _, ChairBack(_)) => () }
        expectNoMsg()
      }
    }

    "raise costs with every 10th item" in {
      val master = system.actorOf(Master.props(classOf[TableTop], 100, 1.milli), "costRaiser")

      val expectedPrices: Vector[BigDecimal] = Vector(100, 105, 110.25, 115.76)
      for (i <- 1 until 40) {
        val expectedPrice = expectedPrices(i / 10)

        master ! PartRequest(i, classOf[TableTop])
        blocking {
          expectMsgPF() {
            case PartComplete(`i`, price, TableTop(_)) =>
              if (price != expectedPrice) fail(s"On order #$i expected price $expectedPrice but received $price.")
          }
        }
      }
    }

    "sell table legs for twice the price of chair legs" in {
      val master = system.actorOf(Master.props(classOf[Leg], defaultPrice, 1.milli), "tableTwiceSelles")
      master ! PartRequest(1L, classOf[ChairLeg])
      expectMsgPF() { case PartComplete(1L, price, ChairLeg(_)) => price shouldBe defaultPrice }

      master ! PartRequest(2L, classOf[TableLeg])
      expectMsgPF() { case PartComplete(2L, price, TableLeg(_)) => price shouldBe (defaultPrice * 2) }
    }

  }

}
