import doobie.imports._
import scalaz._, Scalaz._


object Select {

	val xa = DriverManagerTransactor[IOLite]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

	def main(args:Array[String]):Unit = {
		getRowsAll
		getRowsFive
		shortQuery
		multiColumn
		shapelessDef
	}

	/**
	 * Get all Columns, then filter
	 */
	def getRowsAll = (sql"select name from country".query[String]     // Query0[String]
		.list              // ConnectionIO[List[String]]
		.transact(xa)      // IOLite[List[String]]
		.unsafePerformIO   // List[String]
		.take(5).foreach(println))

	/**
	 * Get just 5 columns
	 */
	def getRowsFive = (sql"select name from country".query[String]     // Query0[String]
		.process           // Process[ConnectionIO, String]
		.take(5)           // Process[ConnectionIO, String]
		.list              // ConnectionIO[List[String]]
		.transact(xa)      // IOLite[List[String]]
		.unsafePerformIO   // List[String]
		.foreach(println))

	import xa.yolo._

	/**
	 *A little bit shorter
	 */
	def shortQuery = (sql"select name from country".query[String] // Query0[String]
		.process       // Process[ConnectionIO, String]
		.take(5)       // Process[ConnectionIO, String]
		.quick         // Task[Unit]
		.unsafePerformIO)

	/**
	 * 
	 */
	def multiColumn = (sql"select code, name, population, gnp from country".query[(String, String, Int, Option[Double])]
		.process.take(5).quick.unsafePerformIO)

	import shapeless._

	/**
	 * Tuple defined by H-List
	 */
	def shapelessDef = (sql"select code, name, population, gnp from country".query[String :: String :: Int :: Option[Double] :: HNil]
		.process.take(5).quick.unsafePerformIO)



	/**
	 * Shpeless Record 
	 */
	import shapeless.record.Record
	type Rec = Record.`'code -> String, 'name -> String, 'pop -> Int, 'gnp -> Option[Double]`.T
	def shapelessRec = (sql"select code, name, population, gnp from country"
        .query[Rec]
        .process.take(5).quick.unsafePerformIO)



}