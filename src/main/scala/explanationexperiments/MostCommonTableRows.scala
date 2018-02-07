package explanationexperiments

import data.question.{ExamQuestionParserDynamic, MCExplQuestion}
import edu.arizona.sista.struct.Counter
import edu.arizona.sista.utils.StringUtils
import explanationexperiments.CharacterizeConnectivity.{convertToExplQuestions, filterQuestionsByFlags}
import explanationexperiments.SummaryKnowledgeGrowth.{PROGRAM_TITLE, calculateTableUseSummary}
import explanationgraph.TableStore

import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks.breakable

/**
  * The table usage summary generated first provides the data for Table 3 (the proportion of explanations that contain knowledge from a given table) in LREC 2018.
  * The table row frequency data is not discussed at length in the paper, but shows the most freuquent (and least frequent) rows, and essentially illustrates Zipf's law for table rows.
  * Created by peter on 7/23/17.
  */

object MostCommonTableRows {
  val VERSION = "1.0"
  val PROGRAM_TITLE = "WorldTree: MostCommonTableRows version " + VERSION

  /*
   * Frequency counting
   */
  def findMostFrequentRows(in:Array[MCExplQuestion]):Array[(String, Double)] = {
    val countUID = new Counter[String]

    println (" * findMostFrequentRows: started...")

    // For each question
    for (i <- 0 until in.size) {
      val explQuestion = in(i)
      val explRows = explQuestion.expl

      // For each row/sentence in an given question's explanation graph
      for (j <- 0 until explRows.size) {
        val explRow = explRows(j)
        val uid = explRow.uid
        val role = explRow.role

        countUID.incrementCount(uid)
      }
    }

    val sorted = countUID.sorted
    println (" * findMostFrequentRows: complete... (counted " + sorted.size + " unique rows)")
    // Return
    sorted.toArray
  }


  def displayMostFrequentRows(freq:Array[(String, Double)], tablestore:TableStore, numDisplay:Int = 25): Unit = {
    println(" * displayMostFrequentRows: ")
    var numDisplay1: Int = numDisplay
    if (numDisplay1 == 0) numDisplay1 = freq.size

    var average: Double = 0

    // Header
    println ("Rank\tFrequency\tUUID\tRowText")

    // Data
    for (i <- 0 until numDisplay1) {
      val uid = freq(i)._1
      val count = freq(i)._2
      //val text = tablestore.getRowByUID(uid).toStringDelim(" ")
      val text = tablestore.getRowByUID(uid).toStringText
      println(i + "\t" + count + "\t" + uid + "\t" + text)

      average += count
    }
    average = average / numDisplay1

    if (numDisplay1 == freq.size) {
      println("")
      println("On average, a given table row is used in " + average + " different explanations.")
    }
  }


  /*
   * Helper function
   */
  // Take a set of questions, and return only the questions that have a non-zero value on one or more flags.
  def filterQuestionsByFlags(in:Array[MCExplQuestion], flags:Array[String]):Array[MCExplQuestion] = {
    val out = new ArrayBuffer[MCExplQuestion]

    for (i <- 0 until in.size) {
      breakable {
        for (flag <- flags) {
          if (in(i).question.flags.getCount(flag) > 0) {
            out.append(in(i))
          }
        }
      }
    }

    // Return
    out.toArray
  }



  /*
   * Prints the command line usage information to the console
   */
  def printUsage() {
    println (PROGRAM_TITLE)
    println ("")
    println ("Usage: ... -props myprops.properties")
    println ("")
  }


  /*
   * Main entry point
   */
  def main(args:Array[String]) {
    // Step 1: Check that arguments were specified
    // e.g. " -props myprops.properties"
    if ((args.length == 0)) {
      printUsage()
      System.exit(1)
    }
    val props = StringUtils.argsToProperties(args)


    // Step 2: Load Tablestore
    // Tablestore index filename
    var tablestoreIndex:String = ""
    if (props.getProperty("tablestoreIndex", "") != "") {
      tablestoreIndex = props.getProperty("tablestoreIndex", "")
    } else {
      throw new RuntimeException("ERROR: Unable to find 'tablestoreIndex' property in properties file.")
    }

    // Load tablestore
    val tablestore = new TableStore(tablestoreIndex)
    println ( tablestore.tables( tablestore.UIDtoTableLUT("a5c9-d7a4-8421-bb2e") ).name )
    println ( tablestore.getRowByUID("a5c9-d7a4-8421-bb2e") )


    // Step 3: Load questions
    // Find question filename
    var filenameQuestions:String = ""
    if (props.getProperty("questions", "") != "") {
      filenameQuestions = props.getProperty("questions", "")
    } else {
      throw new RuntimeException("ERROR: Unable to find 'questions' property in properties file.")
    }

    // Load questions
    var questions = ExamQuestionParserDynamic.loadQuestionsFromCSVList( filenameQuestions, fullAnnotation = false, noAnnotation = true, tsvMode = true)
    var explQuestions = convertToExplQuestions(questions)


    println ("Displaying first 10 questions (debug): ")
    for (i <- 0 until 10) {
      println (explQuestions(i).toString)
      println ("")
    }

    // Step 3A: Filter questions to only those that have been tagged as successfully annotated
    val filteredQuestions = filterQuestionsByFlags(explQuestions, Array("SUCCESS", "READY") )
    println ("Loaded " + filteredQuestions.size + " questions after filtering. ")


    // Step 4: Compute The proportion of explanations that contain knowledge from a given table (Table 3 in LREC 2018 paper)
    calculateTableUseSummary(filteredQuestions, tablestore)

    println ("\n\n")

    // Step 5: Compute frequency statistics for table rows
    val freqRows = findMostFrequentRows(filteredQuestions)
    displayMostFrequentRows(freqRows, tablestore, numDisplay = 0)
  }

}
