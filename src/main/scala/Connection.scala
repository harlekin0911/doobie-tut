import doobie.imports._
import scalaz._, Scalaz._



object Connection {
  
  def main( args : Array[String]) : Unit  = {
    
		  val xa = DriverManagerTransactor[IOLite]("org.postgresql.Driver", "jdbc:postgresql:postgres", "postgres", "")
		  val program1 = 42.pure[ConnectionIO]
		  val task = program1.transact(xa)
		  
		  println( task)
		  
		  
		   val program2 = sql"select 42".query[Int].unique
		   val task2 = program2.transact(xa)
		   val o = task2.unsafePerformIO
		   
		   println(o)



  }

}