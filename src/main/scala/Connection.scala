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


		   val program3 = for {
			     a <- sql"select 42".query[Int].unique
			     b <- sql"select random()".query[Double].unique
		   } yield (a, b)

		   println(program3)
		   
		   println( program3.transact(xa).unsafePerformIO)
		   
		   val program3a = {
           val a = sql"select 42".query[Int].unique
           val b = sql"select random()".query[Double].unique
           (a |@| b).tupled
       }

		   program3a.replicateM(5).transact(xa).unsafePerformIO.foreach(println)


		   val kleisli = program1.transK[IOLite] 
		   val task4 = IOLite.primitive(null: java.sql.Connection) >>= kleisli.run
		   task4.unsafePerformIO

  }

}