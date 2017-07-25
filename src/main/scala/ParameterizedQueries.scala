
/**
 * In this chapter we learn how to construct parameterized queries, and introduce the Composite typeclass
 */
object ParameterizedQueries {
    
    import doobie.imports._
    import scalaz._, Scalaz._
    
    val xa = DriverManagerTransactor[IOLite]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
    import xa.yolo._

    case class Country(code: String, name: String, pop: Int, gnp: Option[Double])


    (sql"select code, name, population, gnp from country".query[Country].process.take(5).quick.unsafePerformIO)
    
    /**
     * So let’s factor our query into a method and add a parameter that selects only the countries with a population larger than some value the user will provide. 
     * We insert the minPop argument into our SQL statement as $minPop, just as if we were doing string interpolation.
     */
    def biggerThan(minPop: Int) = sql"""select code, name, population, gnp from country where population > $minPop """.query[Country]
    
    
    /**
     * So what’s going on? It looks like we’re just dropping a string literal into our SQL string, but actually we’re constructing a proper parameterized PreparedStatement, and the minProp value is ultimately set via a call to setInt (see “Diving Deeper” below). 
     * doobie allows you to interpolate values of any type with a Atom instance, which includes
     *    • any JVM type that has a target mapping defined by the JDBC specification,
     *    • vendor-specific types defined by extension packages,
     *    • custom column types that you define, and
     *    • single-member products (case classes, typically) of any of the above.
     *
     *   We will discuss custom type mappings in a later chapter.
     * 
     */
    def runBiggerThan = biggerThan(150000000).quick.unsafePerformIO // Let's see them all



    /**
     * Multiple Parameters
     * 
     */
    
    
    def populationIn(range: Range) = sql"""select code, name, population, gnp from country where population > ${range.min} and   population < ${range.max}""".query[Country]

    def runPopulationIn =  populationIn(150000000 to 200000000).quick.unsafePerformIO

    
    /**
     * Dealing with IN Clauses
     * 
     * A common irritant when dealing with SQL literals is the desire to inline a sequence of arguments into an IN clause, 
     * but SQL does not support this notion (nor does JDBC do anything to assist). doobie supports this via statement fragments (see Chapter 8).
     */
    
    def populationIn(range: Range, codes: NonEmptyList[String]) = {
        val q = fr"""select code, name, population, gnp from country where population > ${range.min} and   population < ${range.max} and   """ ++ Fragments.in(fr"code", codes) // code IN (...)
        q.query[Country]
    }


    /**
     * Note that the IN clause must be non-empty, so codes is a NonEmptyList.
     * Running this query gives us the desired result.
     * 
     */
    def runPopulationIn2 = populationIn(100000000 to 300000000, NonEmptyList("USA", "BRA", "PAK", "GBR")).quick.unsafePerformIO

    
    /**
     * Diving Deeper
     * 
     * In the previous chapter’s Diving Deeper we saw how a query constructed with the sql interpolator is just sugar for the process constructor 
     * defined in the doobie.hi.connection module (aliased as HC). Here we see that the second parameter, a PreparedStatementIO program, is used to set the query parameters. 
     * The third parameter specifies a chunking factor; rows are buffered in chunks of the specified size.
     */
    
    
    
    val q = """select code, name, population, gnp from country where population > ? and   population < ? """
    import scalaz.stream.Process
    def proc(range: Range): Process[ConnectionIO, Country] = HC.process[Country](q, HPS.set((range.min, range.max)), 512)


    /**
     * But how does the set constructor work?
     * When reading a row or setting parameters in the high-level API, we require an instance of Composite[A] for the input or output type. 
     * It is not immediately obvious when using the sql interpolator, but the parameters (each of which require an Atom instance, to be discussed in a later chapter) 
     * are gathered into an HList and treated as a single composite parameter.
     * 
     * Composite instances are derived automatically for column types that have Atom instances, and for products of other composites (via shapeless.ProductTypeclass). 
     * We can summon their instances thus:
     * 
     */
    
    val dd = Composite[(String, Boolean)]
    //Composite[Country]

    /**
     * The set constructor takes an argument of any type with a Composite instance and 
     * returns a program that sets the unrolled sequence of values starting at parameter index 1 by default. Some other variations are shown here.
     */
    
    // Set parameters as (String, Boolean) starting at index 1 (default)
    val a = HPS.set(("foo", true))

    // Set parameters as (String, Boolean) starting at index 1 (explicit)
    HPS.set(1, ("foo", true))

    // Set parameters individually
    HPS.set(1, "foo") *> HPS.set(2, true)

    // Or out of order, who cares?
    HPS.set(2, true) *> HPS.set(1, "foo")



    /**
     * Using the low level doobie.free constructors there is no typeclass-driven type mapping, so each parameter type requires a distinct method, 
     * exactly as in the underlying JDBC API. The purpose of the Atom typeclass (discussed in a later chapter) is to abstract away these differences.
     */
    
    FPS.setString(1, "foo") *> FPS.setBoolean(2, true)

    
    
    
    def main(args:Array[String]):Unit = {
        
        runBiggerThan
        runPopulationIn
        runPopulationIn2
    }
 
}