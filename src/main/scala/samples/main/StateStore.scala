package samples.main

import org.scalajs.dom.document
import rxscalajs.Observable

import scala.scalajs.js

object StateStore extends js.JSApp {

  case class Person(firstName: String, lastName: String)
  val initialState = List(Person("John","Doe"))

  val addPerson = Observable.fromEvent(document.querySelector("#add"), "click")
    .mapTo((list: List[Person]) => list :+ Person("Jane","Doe"))

  val deletePerson = Observable.fromEvent(document.querySelector("#delete"), "click")
    .mapTo((list: List[Person]) => list.dropRight(1))


  val state = addPerson.merge(deletePerson)
    .scan(initialState)((state, modify) => modify(state))
    .startWith(initialState)

  def main(): Unit = {
    state.subscribe(list => {

      val domList = list.map(person => {
        val li = document.createElement("li")
        li.innerHTML = s"${person.firstName} ${person.lastName}"
        li
      })

      val ul = document.querySelector("#list")

      ul.innerHTML = ""

      domList.foreach(ul.appendChild)

    })
  }

}
