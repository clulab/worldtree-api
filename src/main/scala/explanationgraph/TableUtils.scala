package explanationgraph

import data.question.MCExplQuestion
import edu.arizona.sista.struct.Counter

import scala.collection.mutable.ArrayBuffer

/**
  * Created by peter on 9/7/17.
  */
class TableUtils {

}



object TableUtils {
  lazy val lookupLemmatizer = LookupLemmatizer

  /*
   * Conversion from MCExplQuestion
   */
  // From an MCExplQuestion, generate an array of TableRows representing the explanation for that question
  def getExplanationRows(in:MCExplQuestion, tablestore:TableStore):Array[TableRow] = {
    val out = new ArrayBuffer[TableRow]
    val expl = in.expl

    for (i <- 0 until expl.size) {
      val uid = expl(i).uid
      val row = tablestore.getRowByUID(uid)
      out.append(row)
    }

    // Return
    out.toArray
  }


  /*
   * Overlap
   */

  // Determine whether (and, if so, how) two table rows lexically overlap.
  def determineRowOverlap(row1:TableRow, row2:TableRow):(Counter[String], Counter[String], Counter[String]) = {
    // Step 1: Broad lexical overlap
    val overlappingLemmas = determineRowOverlapLexical(row1, row2)
    println ("Overlapping lemmas: " + overlappingLemmas)

    // Step 2: Cell lexical overlap
    val connCounterCell = new Counter[String]
    val connCounterLex = new Counter[String]
    val connCounterTable = new Counter[String]

    val lexicallyConnectedCells = determineRowConnectionLexical(row1, row2)
    for (i <- 0 until lexicallyConnectedCells.size) {
      val connection = lexicallyConnectedCells(i)
      val lexicalOverlap = connection._1
      val row1CellIdx = connection._2
      val row2CellIdx = connection._3

      val row1CellHeaderStr = row1.header(row1CellIdx)
      val row2CellHeaderStr = row2.header(row2CellIdx)

      println ("Cell-to-Cell connection: Row 1 : Cell " + row1CellIdx + "(" + row1CellHeaderStr + ", " + row1.tableName + ") <--> Row 2 : Cell " + row2CellIdx + "(" + row2CellHeaderStr + ", " + row2.tableName + ")  :: Overlap: " + lexicalOverlap )

      // Connection characterization
      val connStrCell = "(" + row1.tableName + ", " + row1CellHeaderStr + ") <--> (" + row2.tableName + ", " + row2CellHeaderStr + ")"
      val connStrLex = "(" + row1.tableName + ", " + row1CellHeaderStr + ") <--> (" + row2.tableName + ", " + row2CellHeaderStr + ")  ::  Overlap: " + lexicalOverlap
      val connStrTable = "(" + row1.tableName + ") <--> (" + row2.tableName + ")"
      /*
      //## OLD: (One count per word that overlaps)
      connCounterCell.incrementCount(connStrCell)
      connCounterLex.incrementCount(connStrLex)
      connCounterTable.incrementCount(connStrTable)
      */
      //## NEW: (One count per cell, regardless of how many words overlap)
      connCounterCell.setCount(connStrCell, 1.0)
      connCounterLex.setCount(connStrLex, 1.0)
      connCounterTable.setCount(connStrTable, 1.0)
    }

    // Return
    (connCounterCell, connCounterLex, connCounterTable)
  }


  // Determine the set of words that two rows have in common
  def determineRowOverlapLexical(row1:TableRow, row2:TableRow):Set[String] = {
    val minLength:Int = 2

    // Step 1: Get indicies of 'data' columns from each row
    val activeColsRow1 = row1.dataColumns
    val activeColsRow2 = row2.dataColumns


    // Step 2: Retrieve lemmas for each row
    var lemmasRow1:Set[String] = Set()
    var lemmasRow2:Set[String] = Set()

    // Collect bag-of-word lemmas for row1
    for (colIdx1 <- activeColsRow1) {
      for (word <- row1.getCellWords(colIdx1)) {
        val lemma = coarseLemmatize(word)
        if ((lemma.length() >= minLength) && !isStopword(lemma)) {
          lemmasRow1 += lemma
        }
      }
    }

    // Collect bag-of-word lemmas for row1
    for (colIdx2 <- activeColsRow2) {
      for (word <- row2.getCellWords(colIdx2)) {
        val lemma = coarseLemmatize(word)
        if ((lemma.length() >= minLength) && !isStopword(lemma)) {
          lemmasRow2 += lemma
        }
      }
    }


    // Step 3: Compare overlap
    val commonLemmas = lemmasRow1.intersect(lemmasRow2)

    // Return
    commonLemmas
  }


  // Determine the set of words that two rows have in common
  def determineRowConnectionLexical(row1:TableRow, row2:TableRow):Array[(Set[String], Int, Int)] = {
    val minLength:Int = 2
    val connections = new ArrayBuffer[(Set[String], Int, Int)]    // (overlapping words, columnIndexRow1, columnIndexRow2)

    // Step 1: Get indicies of 'data' columns from each row
    val activeColsRow1 = row1.dataColumns
    val activeColsRow2 = row2.dataColumns


    // Step 2: Compare lemma overlap between both rows, cell-by-cell.
    var lemmasRow2:Set[String] = Set()

    // Step 2A: For each active cell in Row1
    for (colIdx1 <- activeColsRow1) {

      // Step 2B: Collect the lemmas in a given cell in Row1
      var lemmasCell1:Set[String] = Set()
      for (word <- row1.getCellWords(colIdx1)) {
        val lemma = coarseLemmatize(word)
        if ((lemma.length() >= minLength) && !isStopword(lemma)) {
          lemmasCell1 += lemma
        }
      }

      // Step 2C: For each active cell in Row2
      for (colIdx2 <- activeColsRow2) {
        // Step 2D: Collect the lemmas in a given cell in Row2
        var lemmasCell2:Set[String] = Set()
        for (word <- row2.getCellWords(colIdx2)) {
          val lemma = coarseLemmatize(word)
          if ((lemma.length() >= minLength) && !isStopword(lemma)) {
            lemmasCell2 += lemma
          }
        }

        // Step 2E: Compare those lemmas for overlap
        val cellOverlap = lemmasCell1.intersect(lemmasCell2)
        if (cellOverlap.nonEmpty) {
          connections.append( (cellOverlap, colIdx1, colIdx2) )
        }
      }
    }

    // Step 3: Return
    connections.toArray

  }


  /*
   * Combinations
   */
  // Make all (unique) combinations of N indices up to length N.
  // Normally this would produce only a 2 dimensional array, but here the third (outermost) dimension stores all the
  // patterns of length 1, length, 2, length 3, etc.
  // Runtime is ~20msec up to length 10, ~100msec up to length 17, and ~500msec for length 20.
  def mkCombinations(n:Int):Array[Array[Array[Int]]] = {
    // Step 1: Create indicies
    val indices = (0 to n-1) toArray

    // Output array (Length, Combinations, Pattern)
    val out = new ArrayBuffer[Array[Array[Int]]]

    // Make combinations for a given length
    for (i <- 1 to n) {
      val combosAtLength = indices.combinations(i).toArray
      out.append(combosAtLength)
    }

    // Return
    out.toArray
  }


  /*
   * Helper functions
   */
  def isStopword(in:String):Boolean = {
    var stopWords = Array("a", "an", "and", "are", "as", "at", "be", "for", "have", "he", "she", "in", "is", "it", "its", "of", "that", "the", "then", "was", "were", "will", "with", "kind", "mean", "who", "what", "where", "why", "when", "how", "to")
    if (stopWords.contains(in)) return true

    // Return (default)
    false
  }

  // Coarse look-up lemmatizer
  def coarseLemmatize(in:String):String = {
    // TODO: Implement
    //return in
    lookupLemmatizer.getLemma(in)
  }

}


