package explanationgraph

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by user on 7/9/17.
  */

class TableStore(filename:String) {
  val tables = new ArrayBuffer[Table]
  val UIDtoTableLUT = mutable.Map[String, Int]().withDefaultValue(-1)

  /*
   * Constructor
   */
  loadTableStore(filename)


  /*
   * Finding rows
   */
  def getRowTableByUID(uid:String):Int = {
    UIDtoTableLUT(uid)
  }

  def getRowByUID(uid:String):TableRow = {
    val tableIdx = getRowTableByUID(uid)
    if (tableIdx == -1) {
      // ERROR
      println ("ERROR: Could not find UID: " + uid + " (returning empty table row)")
      return new TableRow("", Array.empty[String], Array.empty[Int], Array.empty[String])
    }
    val table = tables(tableIdx)
    table.getRowByUID(uid)
  }


  /*
   * Finding tables
   */
  def findTableIdxByName(name:String):Int = {
    for (i <- 0 until tables.size) {
      if (tables(i).name.toLowerCase == name.toLowerCase) {
        return i
      }
    }

    // Default: table could not be found
    -1
  }


  /*
   * Loading the tablestore
   */

  // Load a set of tables from a text file specifying the filenames of each table, one per line.
  def loadTableStore(filename:String): Unit = {
    println (" * loadTableStore: Started... (filename index = " + filename + ")")
    val tableRelativePath = filename.substring(0, filename.lastIndexOf("/") + 1) + "tables/"

    var numRows:Int = 0
    for (line <- io.Source.fromFile(filename, "UTF-8").getLines()) {
      val filenameTable = tableRelativePath + line
      if (addTable(filenameTable)) {
        numRows += tables.last.numRows
      }
    }

    println (" * loadTableStore: Complete. (" + tables.size + " tables loaded, containing a total of " + numRows + " rows)")
  }

  // Add a single table to the tablestore.  If the table is invalid, it will not be added.
  def addTable(filename:String):Boolean = {
    val table = new Table(filename)

    if (table.valid) {
      tables.append(table)

      // Display table information
      println("\t" + tables.last.toString)

      // Add UIDs
      val tableIdx = tables.size - 1
      val UIDsInTable = table.getAllTableUIDs()
      for (i <- 0 until UIDsInTable.size) {
        val uid = UIDsInTable(i)
        UIDtoTableLUT(uid) = tableIdx
      }

      return true
    }

    // Return
    false
  }



}


object TableStore {

  // Example usage
  def main(args: Array[String]): Unit = {
    val tablestore = new TableStore("annotation/expl-tablestore-export-2017-07-09-160303/tableindex.txt")

    println ( tablestore.tables( tablestore.UIDtoTableLUT("a5c9-d7a4-8421-bb2e") ).name )

    println ( tablestore.getRowByUID("a5c9-d7a4-8421-bb2e") )

  }


}
