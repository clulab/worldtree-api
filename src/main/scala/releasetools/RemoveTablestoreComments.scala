package releasetools

import java.io.PrintWriter
import scala.collection.mutable.ArrayBuffer

/**
  * This quick tool will strip all comments from a tablestore export, in preparation for public release
  * Created by peter on 9/20/17.
  */

object RemoveTablestoreComments {

  // Find the comment column in a given tablestore header
  def getCommentColumnIdx(in:Array[String]):Int = {
    println (in.mkString(", "))
    for (i <- 0 until in.size) {
      if (in(i).trim().toUpperCase().startsWith("[SKIP] COMMENT")) {
        return i
      }
    }
    // Default return
    -1
  }

  // Remove the comments from a given table
  def removeComments(path:String, filename:String) = {
    println (" * removeComments (filename = " + filename + ")")

    val lines = io.Source.fromFile(path + filename, "UTF-8").getLines()
    val out = new ArrayBuffer[Array[String]]

    // Step 1: Read in table data
    var lineNum:Int = 0
    var colIdx:Int = -1
    for(line <- lines) {
      val fields = line.split("\t")

      // If header line, then find the comment column index
      if (lineNum == 0) {
        colIdx = getCommentColumnIdx(fields)
        out.append( fields )
        println ("\t* comment column index: " + colIdx)
      } else {
        if (colIdx >= 0) {
          fields(colIdx) = ""
          out.append( fields )
        }
      }

      lineNum += 1
    }

    // Step 2: Export table data
    if (colIdx >= 0) {
      val pw = new PrintWriter(path + filename)
      for (line <- out) {
        pw.println( line.mkString("\t") )
      }
      pw.close()

      println ("\t* wrote " + out.size + " lines. ")
    }

  }


  // Load the tablestore index file
  def getTablestoreFilenames(path:String, filename:String):Array[String] = {
    val out = new ArrayBuffer[String]

    for (line <- io.Source.fromFile(path + filename, "UTF-8").getLines()) {
      out.append( line )
    }

    out.toArray
  }



  def main(args: Array[String]): Unit = {
    val path = "/home/user/Documents/tsv/"
    val pathTables = "/home/user/Documents/tsv/tables/"
    val filenameIndex = "tableindex.txt"


    val filenamesTables = getTablestoreFilenames(path, filenameIndex)
    for (filenameTable <- filenamesTables) {
      removeComments(pathTables, filenameTable)
    }


    println ("")
    println (filenamesTables.size + " tables listed in index.")
  }

}
