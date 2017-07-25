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
		shapelessRec
		caseClass
		caseClassProdukt
		caseProductToMap
		getProcess
		explicitSql
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

    /**
     * YOLO Mode 
     * 
     * The API we have seen so far is ok, but it’s tiresome to keep saying transact(xa) and doing foreach(println) to see what the results look like. 
     * So just for REPL exploration there is a module of extra syntax provided on Transactor that you can import, and it gives you some shortcuts.
	 */
	import xa.yolo._

	/**
	 *A little bit shorter implicit to system out by quick
	 * 
	 * This syntax allows you to quickly run a Query0[A] or Process[ConnectionIO, A] and see the results printed to the console. 
	 * This isn’t a huge deal but it can save you some keystrokes when you’re just messing around.
     *  • The .quick method sinks the stream to standard out (adding ANSI coloring for fun) and then calls .transact, yielding a Task[Unit].
     *  • The .check method returns a Task[Unit] that performs a metadata analysis on the provided query and asserted types and prints out a report. 
     *    This is covered in detail in the chapter on typechecking queries.
     *  
	 * 
	 */
	def shortQuery = (sql"select name from country".query[String] // Query0[String]
		.process       // Process[ConnectionIO, String]
		.take(5)       // Process[ConnectionIO, String]
		.quick         // Task[Unit]
		.unsafePerformIO)

	/**
	 * Multi Column
	 * 
	 * We can select multiple columns, of course, and map them to a tuple. 
	 * The gnp column in our table is nullable so we’ll select that one into an Option[Double]. 
	 * In a later chapter we’ll see how to check the types to be sure they’re sensible.
	 */
	def multiColumn = (sql"select code, name, population, gnp from country".query[(String, String, Int, Option[Double])]
		.process.take(5).quick.unsafePerformIO)

	import shapeless._

	/**
	 * Tuple defined by H-List
	 * 
	 * doobie automatically supports row mappings for atomic column types, as well as options, tuples, 
	 * HLists, shapeless records, and case classes thereof. So let’s try the same query with an HList:
	 */
	def shapelessDef = (sql"select code, name, population, gnp from country".query[String :: String :: Int :: Option[Double] :: HNil]
		.process.take(5).quick.unsafePerformIO)



	/**
	 * Shapeless Record 
	 */
	import shapeless.record.Record
	type Rec = Record.`'code -> String, 'name -> String, 'pop -> Int, 'gnp -> Option[Double]`.T
	def shapelessRec = (sql"select code, name, population, gnp from country"
        .query[Rec]
        .process.take(5).quick.unsafePerformIO)


    /**
     * Case - Class
     * 
     * And again, mapping rows to a case class
     */
    case class Country(code: String, name: String, pop: Int, gnp: Option[Double])
    
    def caseClass = (sql"select code, name, population, gnp from country"
        .query[Country] // Query0[Country]
        .process.take(5).quick.unsafePerformIO)

    /**
     * Produkt
     * 
     * You can also nest case classes, HLists, shapeless records, and/or tuples arbitrarily as long as the eventual members are of supported columns types. 
     * For instance, here we map the same set of columns to a tuple of two case classes
     */
    case class Code(code: String)
    case class Country2(name: String, pop: Int, gnp: Option[Double])

    def caseClassProdukt = (sql"select code, name, population, gnp from country"
        .query[(Code, Country2)] // Query0[(Code, Country2)]
        .process.take(5).quick.unsafePerformIO)

    def caseProductToMap = (sql"select code, name, population, gnp from country"
         .query[(Code, Country2)] // Query0[(Code, Country2)]
         .process.take(5)        // Process[ConnectionIO, (Code, Country)]
         .list                   // ConnectionIO[List[(Code, Country)]]
         .map(_.toMap)           // ConnectionIO[Map[Code, Country]]
         .quick.unsafePerformIO)

   /**
     * In the examples above we construct a Process[ConnectionIO, A] and discharge it via .list (which is just shorthand for .runLog.map(_.toList)), yielding a ConnectionIO[List[A]] 
     * which eventually becomes a Task[List[A]]. So the construction and execution of the Process is entirely internal to the doobie program.
     * However in some cases a stream is what we want as our “top level” type. For example, 
     * http4s can use a Process[Task, A] directly as a response type, which could allow us to stream a resultset directly to the network socket. 
     * We can achieve this in doobie by calling transact directly on the Process[ConnectionIO, A].
     *  
     */
    def getProcess = {
	    
	    val p = sql"select name, population, gnp from country"
            .query[Country2]  // Query0[Country]
            .process         // Process[ConnectionIO, Country]
            .transact(xa)    // Process[Task, Country]
       

        p.take(5).runLog.unsafePerformIO.foreach(println)
	}
	
	def explicitSql = {
	    val sql = "select code, name, population, gnp from country"
        val proc = HC.process[(Code, Country2)](sql, ().pure[PreparedStatementIO], 512) // chunk size
        // Process[ConnectionIO, (Code, Country2)], ConnectionIO[List[(Code, Country2)]], ConnectionIO[Map[Code, Country]]
        proc.take(5).list.map(_.toMap).quick.unsafePerformIO
	}


}