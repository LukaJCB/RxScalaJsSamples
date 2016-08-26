package samples.spaceship


import org.scalajs.dom.{Event, document, window}
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.{KeyboardEvent, CanvasRenderingContext2D, MouseEvent}
import rxscalajs._

import scala.collection.mutable.ArrayBuffer
import scala.scalajs.js

trait GameObject { def x: Double; def y: Double }
case class Enemy(var x: Double, var y: Double, var isDead: Boolean, var shots: ArrayBuffer[Shot]) extends GameObject
case class Shot(var x: Double, var y: Double) extends GameObject
case class Player(var x: Double, var y: Double) extends GameObject
case class Star(var x: Double, var y: Double, size: Double) extends GameObject
case class HeroShot(var x: Double, var y: Double) extends GameObject
case class Actors(
    stars: Seq[Star],
    spaceship: Player,
    enemies: Seq[Enemy],
    heroShots: Seq[HeroShot],
    score: Int)

object Game extends js.JSApp {



  val canvas = document.createElement("canvas").asInstanceOf[Canvas]
  val ctx = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
  document.body.appendChild(canvas)
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight


  /* Constants */
  val SPEED = 40
  val SHOOTING_FREQ = 250
  val SHOOTING_SPEED = 15
  val ENEMY_FREQ = 1500
  val ENEMY_SHOOTING_FREQ = 750
  val SCORE_INCREASE = 10
  val HERO_Y = canvas.height - 30

  /* Helper defs */
  def getRandomInt(min: Int, max: Int) = {
     Math.floor(Math.random() * (max - min + 1)) + min
  }

  def collision(target1: GameObject, target2: GameObject)= {
     (target1.x > target2.x - 20 && target1.x < target2.x + 20) &&
      (target1.y > target2.y - 20 && target1.y < target2.y + 20)
  }

  def drawTriangle(x: Double, y: Double, width: Double, color: js.Any, direction: String) = {
    ctx.fillStyle = color
    ctx.beginPath()
    ctx.moveTo(x - width, y)
    ctx.lineTo(x, if (direction == "up" ) y - width  else y + width)
    ctx.lineTo(x + width, y)
    ctx.lineTo(x - width,y)
    ctx.fill()
  }

  def paintStars(stars: Seq[Star]) {
    ctx.fillStyle = "#000000"
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.fillStyle = "#ffffff"
    stars.foreach( star => {
      ctx.fillRect(star.x, star.y, star.size, star.size)
    })
  }

  def paintSpaceShip(x: Double, y: Double): Unit = {
    drawTriangle(x, y, 20, "#ff0000", "up")
  }

  def paintEnemies(enemies: Seq[Enemy]) = {
    enemies.foreach( enemy => {
      enemy.y += 5
      enemy.x += getRandomInt(-15, 15)

      if (!enemy.isDead) {
        drawTriangle(enemy.x, enemy.y, 20, "#00ff00", "down")
      }

      enemy.shots.foreach( shot => {
        shot.y += SHOOTING_SPEED
        drawTriangle(shot.x, shot.y, 5, "#00ffff", "down")
      })
    })
  }

  def paintScore(score: Int) = {
    ctx.fillStyle = "#ffffff"
    ctx.font = "bold 26px sans-serif"
    ctx.fillText("Score: " + score, 40, 43)
  }

  def isVisible[T <: GameObject](obj: T) = {
     obj.x > -40 && obj.x < canvas.width + 40 &&
      obj.y > -40 && obj.y < canvas.height + 40
  }

  def paintHeroShots(heroShots: Seq[HeroShot], enemies: Seq[Enemy]) = {
    heroShots.zipWithIndex.foreach{ case (shot: HeroShot, i: Int) => {
      var impact = false
      enemies.zipWithIndex
        .takeWhile(tuple => !impact)
        .foreach{ case (enemy,l) =>
        if (!enemy.isDead && collision(shot, enemy)) {
          ScoreSubject.next(SCORE_INCREASE)
          enemy.isDead = true
          shot.x = -100
          shot.y = -100
          impact = true
        }
      }

      if (!impact) {
        shot.y -= SHOOTING_SPEED
        drawTriangle(shot.x, shot.y, 5, "#ffff00", "up")
      }
    }}
  }

  def gameOver(player: Player, enemies: Seq[Enemy]): Boolean = {
    enemies.exists( enemy => {
      if (collision(player, enemy)) {
         return true
      }

      enemy.shots.exists(shot => collision(player, shot))
    })
  }

  /* Reactive code */



  val keyDownStream =  Observable.fromEvent(canvas, "keydown")
    .filter( evt => {
      val e = evt.asInstanceOf[KeyboardEvent]
      e.keyCode == 32
    })

  val playerShots =
    Observable.fromEvent(canvas, "click") merge keyDownStream


  val StarStream =  Observable.range(1, 250)
    .map(n => Star((Math.random() * canvas.width).toInt,(Math.random() * canvas.height).toInt,Math.random() * 3 + 1))
    .toSeq
    .flatMap( arr => {
      Observable.interval(SPEED).map(n => {
        arr.map( star => {
          if (star.y >= canvas.height) {
            star.y = 0
          }
          star.y += star.size
          star
        })
      })
    })

  val mouseMove =  Observable.fromEvent(canvas, "mousemove")
  val SpaceShip = mouseMove
    .map( event => {
      Player( event.asInstanceOf[MouseEvent].clientX, HERO_Y)
    })
    .startWith(Player(canvas.width / 2, HERO_Y))

  val playerFiring = playerShots
     .mapTo(())
     .startWith(())
     .sampleTime(200)
     .timestamp

  val HeroShots: Observable[Seq[HeroShot]] = playerFiring
    .combineLatestWith(SpaceShip)((shotEvents, spaceShip) => (spaceShip.x,shotEvents.timestamp))
      .distinct((tuple,tuple2) => tuple._2 == tuple2._2)
    .scan(Seq[HeroShot]())((shots, tuple) => {
      shots :+ HeroShot(tuple._1, HERO_Y) filter (n => isVisible(n))
    })

  val Enemies =  Observable.interval(ENEMY_FREQ)
    .scan(Seq[Enemy]()) ((enemies, n) => {
      val enemy = Enemy(
        (Math.random() * canvas.width).toInt,
         -30,
        isDead = false,
        ArrayBuffer()
      )


       Observable.interval(ENEMY_SHOOTING_FREQ).subscribe( n => {
        if (!enemy.isDead) {
          enemy.shots += Shot(enemy.x, enemy.y)
        }

        enemy.shots = enemy.shots.filter(isVisible)
      })

      (enemies :+ enemy)
        .filter( enemy => {
           isVisible(enemy) && !(enemy.isDead && enemy.shots.isEmpty)
        })
    })

  val ScoreSubject = Subject[Int]()
  val score = ScoreSubject.scan(0)((prev, cur) => {
     prev + cur
  }).startWith(0)

  def renderScene(actors: Actors): Unit = {
    paintStars(actors.stars)
    paintSpaceShip(actors.spaceship.x, actors.spaceship.y)
    paintEnemies(actors.enemies)
    paintHeroShots(actors.heroShots, actors.enemies)
    paintScore(actors.score)
  }

  val Game= (StarStream combineLatest Enemies combineLatest HeroShots combineLatest SpaceShip combineLatest score)
    .map (toActors(_))
    .sampleTime(SPEED)
    .takeWhile( (actors,n) => {
      val isGameOver = gameOver(actors.spaceship, actors.enemies)
      if (isGameOver) {
        actors.enemies.foreach( enemy => {
          enemy.isDead = true
        })
      }
      !isGameOver
    })


  def toActors(tuple: ((((Seq[Star], Seq[Enemy]), Seq[HeroShot]), Player), Int)): Actors = tuple match {
    case (((((stars,enemies),heroshots),player),sc: Int)) =>
      Actors(stars,player,enemies,heroshots,sc)

  }

  def main():Unit = {
    Game.subscribe(renderScene(_))

  }


}
