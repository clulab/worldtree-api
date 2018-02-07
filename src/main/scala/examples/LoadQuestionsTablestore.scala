package examples

import data.question.ExamQuestionParserDynamic
import edu.arizona.sista.utils.StringUtils
import explanationexperiments.CharacterizeConnectivity.convertToExplQuestions
import explanationexperiments.MostCommonTableRows.{filterQuestionsByFlags, printUsage}
import explanationexperiments.SummaryStatistics.PROGRAM_TITLE
import explanationgraph.TableStore

/**
  * Example: Illustrates basic use of loading questions using the question parser, and loading the tablestore.
  * Created by peter on 2/6/18.
  */

object LoadQuestionsTablestore {
  val VERSION = "1.0"
  val PROGRAM_TITLE = "WorldTree: LoadQuestionsTablestore Example Code version " + VERSION


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
  def main(args: Array[String]) {

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
    var explQuestions = convertToExplQuestions(questions)     // Convert from MCQuestion to MCExplQuestion

    // Step 3A: Filter questions to only those that have been tagged as successfully annotated
    val filteredQuestions = filterQuestionsByFlags(explQuestions, Array("SUCCESS", "READY") )
    println ("Loaded " + filteredQuestions.size + " questions after filtering. ")


    /*
     * Example 1: Using the MCQuestion and MCExplQuestion storage classes
     */

    for (i <- 0 until 10) {
      println ("\n\n\nFiltered Question " + i + ":")
      val explQuestion = filteredQuestions(i)     // question is of type MCExplQuestion

      // Print a human-readable formatted summary of the data in the MCExplQuestion storage class
      println(explQuestion.toString())


      // Step 1: Break explQuestion apart to MCQuestion and explanation
      val question = explQuestion.question
      val expl = explQuestion.expl


      // Step 2: Print out some different members of the MCQuestion storage class

      // Print a human-readable formatted summary of the data in the MCQuestion storage class
      println (question.toString)

      // Print out several of the members (see MCQuestion for more details)
      println ("Member access examples: ")
      println ("Question Text: " + question.text)
      println ("Answer Choices: ")
      // For each answer choice
      for (answerIdx <- 0 until question.choices.size) {
        val choice = question.choices(answerIdx)
        println (answerIdx + ": " + choice.text)
      }
      println ("Correct Answer: " + question.correctAnswer)


      /*
       * Example 2: Looking up rows in the Tablestore
       */


      // Step 3: The explanations are stored as a list of universally unique IDs (UUIDs) that reference one or more table rows.
      // In order to recover the text of the explanation, we need to look up these UUIDs in the tablestore.
      // Here is an example of this taking place.

      // For each explanation sentence in the explanation
      println ("Accessing tablestore row examples (retrieving explanation text): ")
      for (explSentIdx <- 0 until expl.size) {
        val explSent = expl(explSentIdx)

        val explSentUUID = explSent.uid     // explanation sentence UUID
        val explSentRole = explSent.role    // role of this explanation sentence (e.g. CENTRAL, GROUNDING, BACKGROUND, LEXICAL GLUE)

        // Look up table row in the tablestore by UUID.  This returns a TableRow storage class.
        // TableRow contains the individual cells, but also has some convenient members if we just want to retrieve the table name or row text as strings.
        val tablerow = tablestore.getRowByUID(explSentUUID)

        // Example: retrieve table name from table row
        val tableName = tablerow.tableName

        // Example: retrieve table row text from table row
        val tablerowText = tablerow.toStringDelim(" ")

        // Print out to console
        println (explSentIdx + "\t" + explSentUUID + "\t" + explSentRole + "\t" + tableName + "\t" + tablerowText)
      }

    }

  }


}
