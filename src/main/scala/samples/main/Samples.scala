package samples.main

import org.scalajs.dom.document
import rxscalajs.Observable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Samples {


  def create = {
    val helloWorldObservable = Observable.create[String](observer => {
      observer.next("Hello")
      observer.next("World")
      observer.complete()
    })
    helloWorldObservable.subscribe(s => println(s))
  }

  def convertFromList = {
    val list = List(1,24,3,35,5,34)
    val listObservable = Observable.from(list)
    listObservable.subscribe(i => println(i))
  }

  def convertFromFuture = {
    val future = Future {
      "HelloFromTheFuture"
    }
    val o = Observable.from(future)
    o.subscribe(n => println(n))
  }

  def ajax = {
    Observable.ajax("https://api.github.com/orgs/reactivex")
      .map(_.response.public_repos)
      .subscribe(n => s"Public Repos: $n")
  }

  def fromDomEvent = {
    Observable.fromEvent(document.getElementById("btn"),"click")
      .mapTo(1)
      .scan(0)(_ + _)
      .subscribe(n => println(s"Clicked $n times"))
  }

}
