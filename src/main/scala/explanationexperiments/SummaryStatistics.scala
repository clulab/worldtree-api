package explanationexperiments

import data.question.{ExamQuestionParserDynamic, MCExplQuestion}
import edu.arizona.sista.utils.StringUtils
import explanationexperiments.CharacterizeConnectivity.{PROGRAM_TITLE, _}

/**
  * Basic summary statistics (number of questions, average explanation length, average rows with a given role per explanation).
  * Created by peter on 8/2/17.
  */


object SummaryStatistics {
  val VERSION = "1.0"
  val PROGRAM_TITLE = "WorldTree: SummaryStatistics version " + VERSION


  def calculateSummaryStatistics(in:Array[MCExplQuestion]): Unit = {
    var numWithExpl:Double = 0.0
    var numNoExpl:Double = 0.0

    var explLength:Double = 0.0

    var numSentCentral:Double = 0.0
    var numSentGrounding:Double = 0.0
    var numSentBackground:Double = 0.0
    var numSentLexicalGlue:Double = 0.0


    // For each question
    for (i <- 0 until in.size) {
      val explQuestion = in(i)
      val question = explQuestion.question
      val expl = in(i).expl

      // Check if it has an explanation populated
      if (expl.size > 0) {
        // Explanations and Explanation Length
        numWithExpl += 1
        explLength += expl.size

        // Explanation roles
        for (j <- 0 until expl.size) {
          val explSent = expl(j)
          if (explSent.role == "CENTRAL") numSentCentral += 1
          if (explSent.role == "GROUNDING") numSentGrounding += 1
          if (explSent.role == "BACKGROUND") numSentBackground += 1
          if (explSent.role == "LEXGLUE") numSentLexicalGlue += 1
        }

      } else {
        // No explanation populated
        numNoExpl += 1
      }
    }


    // Calculate lengths as average length/number per explanation
    explLength = explLength / numWithExpl
    numSentCentral = numSentCentral / numWithExpl
    numSentGrounding = numSentGrounding / numWithExpl
    numSentBackground = numSentBackground / numWithExpl
    numSentLexicalGlue = numSentLexicalGlue / numWithExpl


    // Display
    println ("Summary Statistics: ")
    println ("")
    println ("Number of questions with explanations: " + numWithExpl)
    println ("\twithout explanations: " + numNoExpl)
    println ("")
    println ("Average explanation length: " + explLength)
    println ("")
    println ("Average CENTRAL sentences per explanation: " + numSentCentral)
    println ("Average GROUNDING sentences per explanation: " + numSentGrounding)
    println ("Average BACKGROUND sentences per explanation: " + numSentBackground)
    println ("Average LEXGLUE sentences per explanation: " + numSentLexicalGlue)

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


    // Step 2: Load questions
    // Find question filename
    var filenameQuestions:String = ""
    if (props.getProperty("questions", "") != "") {
      filenameQuestions = props.getProperty("questions", "")
    } else {
      throw new RuntimeException("ERROR: Unable to find 'questions' property in properties file.")
    }

    // Load questions from file
    var questions = ExamQuestionParserDynamic.loadQuestionsFromCSVList( filenameQuestions, fullAnnotation = false, noAnnotation = true, tsvMode = true)
    var explQuestions = convertToExplQuestions(questions)


    println ("Displaying first 10 questions (debug): ")
    for (i <- 0 until 10) {
      println (explQuestions(i).toString)
      println ("")
    }

    // Step 2A: Filter questions to only those that have been tagged as successfully annotated
    val filteredQuestions = filterQuestionsByFlags(explQuestions, Array("SUCCESS", "READY") )

    // Step 3: Calculate summary statistics
    calculateSummaryStatistics(filteredQuestions)
  }


}
