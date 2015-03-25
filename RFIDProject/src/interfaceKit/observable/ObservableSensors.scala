package interfaceKit.observable

import rx.lang.scala.Observable
import rx.lang.scala.schedulers.NewThreadScheduler
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
 * Created by Steven on 18-03-15.
 */
object ObservableSensors
{
  val errorWhatToDo = (ex:Throwable) => ex match
  {
    case ex:InterruptedException =>
    case _ => ex.printStackTrace()
  }

  val hasValueForEachItemOfTuple = (x:(Option[Int], Option[Int])) => x match
  {
    case (Some(x1), Some(x2)) => true
    case _ => false
  }

  def observableTuple(streamSensor0:Stream[Option[Int]], streamSensor1:Stream[Option[Int]]) =
  {
    val obs1 = Observable.from(streamSensor0)
    val obs2 = Observable.from(streamSensor1)
    obs1.zip(obs2)
  }

  def setIntervalToObservable(obs: Observable[(Option[Int], Option[Int])]) =
  {
    Observable.interval(500 milliseconds).take(1).map(x => obs).flatten
  }

  def waitCar(observableTupleWithInterval: Observable[(Option[Int], Option[Int])]) =
  {
    /*
     * When someone has scanned its RFID. The driver has 60 secondes to reach the barrier.
     * If the car don't come in front of the sensors, we don't update the number of places available.
     */
    val endWaitCar = Promise[Boolean]()
    val onNextCoupleWaitCar: ((Option[Int], Option[Int])) => Unit =
    {
      case (Some(result1), Some(result2)) => if(!endWaitCar.isCompleted) endWaitCar.success(true)
      case _ =>
    }

    val subscriptionWaitCar = observableTupleWithInterval.subscribeOn(NewThreadScheduler()).subscribe(onNextCoupleWaitCar, errorWhatToDo)

    Try(Await.ready(endWaitCar.future, 10 seconds)) match
    {
      case Success(x) => println("One car is passing.")
      case Failure(x) => println("Nobody enters in the parking.")
    }

    subscriptionWaitCar.unsubscribe()
  }

  def waitCarToComeIn(observableTupleWithInterval: Observable[(Option[Int], Option[Int])]): Unit =
  {
    /*
     * Afther the attachement has been done. We create 1 data stream for each sensor.
     * Then we create an observable over the values of the sensor 0 and the sensor 1
     */
    val endComeIn = Promise[Boolean]()
    var nbOccurrence = 0
    var nbOccurenceWithoutCar = 0
    var lastCaptorWithInformation = 0

    val onNextCouple: ((Option[Int], Option[Int])) => Unit =
    {
      case (Some(result1), Some(result2)) =>
      {
        println("Value from sensor 0 (cm): " + result1 +" AND Value from sensor1 :" + result2)
        nbOccurrence += 1
      }

      case (Some(result1), None) =>
      {
        println("Value from sensor 0 (cm): " + result1)
        lastCaptorWithInformation = 0
        nbOccurenceWithoutCar = 0
      }

      case (None, Some(result2)) =>
      {
        println("Value from sensor1 :" + result2)
        lastCaptorWithInformation = 1
        nbOccurenceWithoutCar = 0
      }

      case (None, None) =>
      {
        nbOccurenceWithoutCar += 1

        if(nbOccurenceWithoutCar > 50 && !endComeIn.isCompleted)
          endComeIn.success(true) // The car has finished.
      }

      case _ => println("no result")
    }

    val subscriptionCarComeIn = observableTupleWithInterval.subscribeOn(NewThreadScheduler()).subscribe(onNextCouple, errorWhatToDo)

    Await.ready(endComeIn.future, Duration.Inf)

    subscriptionCarComeIn.unsubscribe()

    if(nbOccurrence < 10 || lastCaptorWithInformation == 0)
      println("The car didn't pass the barrier.")
    else
      print("The car has passed the barrier.")
  }
}