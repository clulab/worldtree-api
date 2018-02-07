package explanationgraph
import Table._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by user on 7/9/17.
  */
class Table(filename:String) {
  var name:String = ""
  var header = Array.empty[String]
  var columnRoles = Array.empty[Int]
  var rows = new ArrayBuffer[TableRow]
  val UIDtoRowLUT = mutable.Map[String, Int]().withDefaultValue(-1)

  // Is this table valid and ready to use?
  var valid = false
  val warnings = new ArrayBuffer[String]


  /*
   Constructor
   */
  loadFromFile(filename)


  /*
   Finding rows
   */
  def getRowByUID(uid:String):TableRow = {
    val rowIdx = UIDtoRowLUT(uid)
    rows(rowIdx)
  }


  /*
   * Parse header/rows
   */

  // Read in the table column header (the first line of the tsv file)
  def parseHeader(in:String):Unit = {
    val fields = in.split("\t")
    header = fields
    interpretColumnRoles()

    // Check for valid UID column
    if (findUIDColumnIdx() >= 0) {
      // Found UID column
      valid = true
    } else {
      warnings.append("WARNING: No \"[SKIP] UID\" column found.  This does not appear to be a valid table.")
    }
  }


  // Read in a table row
  def addRow(in:String):Unit = {
    val fields = in.split("\t")
    val UIDColIdx = findUIDColumnIdx()

    // If row has a UID column, and that UID column is populated, then add the row
    if (UIDColIdx >= 0) {
      // Trim all cells
      for (i <- 0 until fields.size) {
        // Merge multiple spaces
        fields(i) = fields(i).replaceAll(" +", " ")

        // Trim
        fields(i) = fields(i).trim()
      }

      // Add row to table
      val rowUID = fields(UIDColIdx)
      if (rowUID.length > 0) {
        rows.append( new TableRow(name, header, columnRoles, fields) ) // Add row
        UIDtoRowLUT(rowUID) = rows.size - 1 // Add UID for row for fast lookup
      }
    }
  }


  // Interpret the roles of each column in the header based off the prefix of the header (fill, skip, uid, etc)
  def interpretColumnRoles():Unit = {
    columnRoles = Array.fill[Int](header.size)(ROLE_UNKNOWN)

    for (i <- 0 until header.size) {
      val colLabel = header(i).trim().toUpperCase
      if (colLabel.startsWith("[FILL]")) {
        columnRoles(i) = ROLE_FILL
      } else if (colLabel.startsWith("[SKIP] UID")) {
        columnRoles(i) = ROLE_UID
      } else if ((colLabel.startsWith("[SKIP]")) || (colLabel.startsWith("#"))) {
        columnRoles(i) = ROLE_SKIP
      } else {
        columnRoles(i) = ROLE_DATA
      }
    }

  }


  /*
   * UID helper functions
   */

  // Return an array of all the UIDs present in this table
  def getAllTableUIDs():Array[String] = {
    UIDtoRowLUT.keySet.toArray
  }

  // Find which column contains the UIDs for each row.  Returns -1 if no UID column was found (signifying an invalid table)
  def findUIDColumnIdx():Int = {
    for (i <- 0 until columnRoles.size) {
      if (columnRoles(i) == ROLE_UID) {
        return i
      }
    }
    // Return
    -1
  }


  /*
   * Accessors
   */
  def numRows():Int = {
    rows.size
  }

  /*
   Load from file
   */
  def loadFromFile(filename:String) = {
    name = filenameToName(filename)

    //println (" * loadFromFile: Loading table... (filename = " + filename + ") ")

    // Load table header/rows
    var lineCount:Int = 0
    for (line <- io.Source.fromFile(filename, "UTF-8").getLines()) {
      if (lineCount == 0) {
        // Header
        parseHeader(line)
      } else {
        // Rows/data
        addRow(line)
      }

      lineCount += 1
    }
  }


  // Helper: Get name of table by stripping path/extension information from filename
  def filenameToName(in:String):String = {
    // Find beginning trim point
    var lastSlash = in.lastIndexOf("/")
    if (lastSlash < 0) lastSlash = -1
    // Find end trim point
    var period = in.indexOf(".", lastSlash)
    if (period < 0) period = in.length()

    // Return
    in.substring(lastSlash + 1, period)
  }


  /*
   * Display
   */
  override def toString:String = {
    val os = new mutable.StringBuilder()

    os.append("Table: " + name.formatted("%35s") + " \tRows: " + rows.size)
    if (valid == false) {
      os.append(" \tValid: " + valid)
    }
    if (warnings.size > 0) {
      os.append(" \tWarnings: " + warnings.mkString(" "))
    }

    os.toString()
  }

  def toStringLong:String = {
    val os = new mutable.StringBuilder()

    os.append("Table: " + name + "  Rows: " + rows.size + "  Valid: " + valid + "  Warnings: " + warnings.mkString(" ") + "\n")
    for (i <- 0 until header.size) {
      os.append( header(i) + " (" + columnRoles(i) + ") \t" )
    }
    os.append("\n")

    for (i <- 0 until rows.size) {
      os.append( "\t" + i + ": " + rows(i).toString + "\n")
    }

    os.toString()
  }

}


object Table {
  /*
   Column Roles
   */
  val ROLE_UNKNOWN    = 0
  val ROLE_DATA       = 1
  val ROLE_FILL       = 2
  val ROLE_SKIP       = 3
  val ROLE_UID        = 4
  val ROLE_API        = 5


  // Example usage
  def main(args: Array[String]): Unit = {
    val table = new Table("annotation/expl-tablestore-export-2017-07-09-160303/tables/USEDFOR.tsv")
    println( table.toStringLong )
  }

}



// Storage class
class TableRow(val tableName:String, val header:Array[String], val columnRoles:Array[Int], val cells:Array[String]) {
  val cellWords = new Array[Array[String]](cells.length)

  // Step 1: Error checking
  if ((header.size != cells.size) || (header.size != columnRoles.size)) {
    throw new RuntimeException("ERROR: TableRow constructor: Size of header, roles, and/or cells array do not match. ")
  }

  // Step 2: Sanitization
  sanitizeCells()

  // Step 3: Pre-compute cell words
  for (i <- 0 until cells.length) {
    cellWords(i) = cells(i).split(" ")
  }

  // Step 3: Identify UID column
  lazy val uidColumnIdx:Int = getUIDColumn()
  lazy val uid:String = getUID()

  // Step 4: Precompute other values that are frequently accessed
  lazy val dataColumns:Array[Int] = getDataColumns()
  lazy val dataAndFillColumns:Array[Int] = getDataAndFillColumns()


  /*
   * Access helpers (getting words from a column)
   */
  def getCellWords(colIdx:Int):Array[String] = {
    // Split on spaces
    //cells(colIdx).split(" ")    // Compute on-the-fly
    // Precomputed (for speed)
    cellWords(colIdx)
  }

  private def getUID():String = {
    if (uidColumnIdx >= 0) {
      return cells(uidColumnIdx)
    } else {
      return "no UID specified"
    }
  }


  /*
   * Access helpers (filtering columns)
   */
  private def getDataColumns():Array[Int] = {
    getColumnsByRole( Array(ROLE_DATA) )
  }

  private def getDataAndFillColumns():Array[Int] = {
    getColumnsByRole( Array(ROLE_DATA, ROLE_FILL) )
  }

  private def getUIDColumn():Int = {
    for (i <- columnRoles.length-1 to 0 by -1) {
      if (columnRoles(i) == ROLE_UID) {
        return i
      }
    }
    -1
  }

  // Return an array of indicies for all columns that are one of the roles provided in 'roles'.
  // Useful for filtering away API/metadata columns, to get only the data columns, or only data/fill columns, etc.
  def getColumnsByRole(roles:Array[Int]):Array[Int] = {
    val out = new ArrayBuffer[Int]
    for (i <- 0 until columnRoles.size) {
      if (roles.contains(columnRoles(i))) {
        out.append(i)
      }
    }
    out.toArray
  }



  /*
   * Sanitization
   */
  def sanitizeCells(): Unit = {
    for (i <- 0 until cells.size) {
      cells(i) = cells(i).replaceAll(";", " ; ")
      cells(i) = cells(i).replaceAll("'s", " 's")
      cells(i) = cells(i).replaceAll("\\s+", " ")
    }
  }


  /*
   * toString methods
   */
  override def toString():String = {
    val os = new mutable.StringBuilder()

    for (i <- 0 until cells.size) {
      os.append(cells(i) + "\t")
    }

    os.toString()
  }

  // toString method with custom delimiter, and merges multiple spaces into a single space.  Useful for converting a row into a plain text sentence.
  def toStringDelim(delim:String):String = {
    val os = new mutable.StringBuilder()

    for (i <- 0 until cells.size) {
      os.append(cells(i) + delim)
    }

    os.toString().replaceAll(" +", " ").trim()
  }

  def toStringSentWithUID():String = {
    val os = new mutable.StringBuilder()

    val columnIdxs = getDataAndFillColumns()
    for (colIdx <- columnIdxs) {
      var text = cells(colIdx)
      if (text.contains(";")) {
        text = "(" + text + ")"
      }
      os.append( text + " " )
    }

    os.append("(UID: " + getUID() + ")")

    os.toString().replaceAll(" +", " ").trim()
  }

  // toString method that just displays the sentence text
  def toStringText():String = {
    val os = new mutable.StringBuilder()

    val colIdxs = getDataAndFillColumns()
    for (colIdx <- colIdxs) {
      os.append( cells(colIdx) + " " )
    }

    // Return
    os.toString().replaceAll(" +", " ").trim()
  }

}