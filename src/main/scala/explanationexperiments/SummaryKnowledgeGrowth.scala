package explanationexperiments

import data.question.{ExamQuestionParserDynamic, MCExplQuestion}
import edu.arizona.sista.struct.Counter
import edu.arizona.sista.utils.StringUtils
import explanationexperiments.CharacterizeConnectivity.{PROGRAM_TITLE, convertToExplQuestions, filterQuestionsByFlags, subsampleQuestions}
import explanationgraph.TableStore

import scala.collection.mutable.ArrayBuffer

/**
  * A monte-carlo analysis to determine the number of unique table rows required to explainably answer a given number of questions.
  * This simulation generates the data for Figure 6 in the LREC2018 paper.
  * The table usage summary generated before the monte-carlo simulation provides the data for Table 3 (the proportion of explanations that contain knowledge from a given table).
  * Created by peter on 8/2/17.
  */

object SummaryKnowledgeGrowth {
  val VERSION = "1.0"
  val PROGRAM_TITLE = "WorldTree: SummaryKnowledgeGrowth version " + VERSION

  /*
   * Summary statistics (number of tables, rows, explanations, distribution of roles (central, grounding, background, lexical glue) )
   */
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
   * Distribution of tables across explanations (e.g. taxonomic, 76%, synonymy, 56%, ...)
   */
  def calculateTableUseSummary(in:Array[MCExplQuestion], tablestore:TableStore): Unit = {
    var numWithExpl:Double = 0.0
    var explLength:Double = 0.0

    var tableUse = new Counter[String]

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

        val tableUseOneQuestion = new Counter[String]

        // Explanation sentences/table rows
        for (j <- 0 until expl.size) {
          val explSent = expl(j)
          val role = explSent.role
          val uid = explSent.uid

          // Store which table this UID/table row/explanation sentence came from
          val tableIdx = tablestore.UIDtoTableLUT(uid)
          if (tableIdx >= 0) {
            val tableName = tablestore.tables(tableIdx).name
            tableUseOneQuestion.setCount(tableName, 1.0)
            //## Debug/verbose mode
            // println (tableName + "\t" + uid)
          } else {
            println ("ERROR: UID not found: " + uid)
          }

        }

        // Add a given question's (binary) counter to the global counter
        for (key <- tableUseOneQuestion.keySet) {
          tableUse.incrementCount(key, 1.0)
        }

      } else {
        // No explanation populated

      }
    }


    // Display counter
    println ("")

    println ("Knowledge Use Summary")

    println ("")
    println ("Prop.\tReuse\tTableName")
    val sorted = tableUse.sorted(descending = true)
    for (i <- 0 until sorted.size) {
      val proportion = sorted(i)._2.toDouble / numWithExpl
      val label = sorted(i)._1

      var reuseProportion:Double = 0.0
      val tableIdx = tablestore.findTableIdxByName(label)
      if (tableIdx >= 0) {
        val numRows:Double = tablestore.tables(tableIdx).numRows()
        reuseProportion = (numWithExpl * proportion) / numRows
        //println (numRows + "\t" + numWithExpl)
      }

      println (proportion.formatted("%3.3f") + "\t" + reuseProportion.formatted("%3.1f") + " \t" + label )
    }



  }



  /*
   * Knowledge growth analysis
   */

  def calculateKnowledgeGrowthMonteCarlo(in:Array[MCExplQuestion], tablestore:TableStore, numSimulations:Int=10, stepSize:Int=50): Unit = {
    var curNumQuestions: Int = stepSize
    if (curNumQuestions > in.size) curNumQuestions = in.size // - 1

    val categories = Array("COMPLEX", "INFSUPP", "RET")

    val data = new ArrayBuffer[Array[Double]]
    val stepSizes = new ArrayBuffer[Int]

    // For each simulation size (a given number of questions)
    println ("\n * calculateKnowledgeGrowthMonteCarlo: numSimulations = " + numSimulations)
    while (curNumQuestions <= in.size) {
      println("curNumQuestions: " + curNumQuestions + " / " + in.size)

      val dist = new Counter[String]
      for (i <- 0 until numSimulations) {

        // Step 1: Subsample questions
        val subsampledQuestions = subsampleQuestions(in, curNumQuestions)

        // Step 2: Calculate knowledge distribution
        val distSim = calculateKnowledgeDist(subsampledQuestions, tablestore)

        // Average (part 1, sum)
        for (key <- distSim.keySet) {
          dist.incrementCount(key, distSim.getCount(key))
        }

      }

      // Average (part 2, divide)
      for (key <- dist.keySet) {
        dist.setCount(key, dist.getCount(key) / numSimulations.toDouble)
      }


      // Step 3: Store data for this number of questions
      val dataN = Array.fill[Double](categories.size)(0.0)
      for (i <- 0 until categories.size) {
        dataN(i) = dist.getCount(categories(i))
      }

      data.append(dataN)
      stepSizes.append(curNumQuestions)

      // Increment monte carlo simulation size
      curNumQuestions += stepSize

    }

    // Display

    println ("")
    println ("Analysis Results")
    println ("Number of unique table rows to answer a given number of questions. ")
    println ("Note: NUMQ is the X axis")
    println ("'SUM' is the number of unique rows required to explainably answer NUMQ questions, and is the data used for the LREC2018 graph (Figure 6).")
    println ("The other columns break down 'SUM' by table knowledge type using the COLING'2016 types (complex inference, inference supporting, or retrieval)")
    println ("")

    // Header
    val delim:String = "\t"

    print("NUMQ" + delim)
    for (j <- 0 until categories.size) {
      print (categories(j) + "_RAW" + delim)
    }
    print ("SUM" + delim)
    for (j <- 0 until categories.size) {
      print (categories(j) + "_PROP" + delim)
    }
    println ("")

    // Data
    for (i <- 0 until data.size) {
      print (stepSizes(i) + delim)

      // Raw numbers
      for (j <- 0 until categories.size) {
        print (data(i)(j).formatted("%3.1f") + delim)
      }

      // Normalized proportions
      var sum:Double = 0.0
      for (j <- 0 until categories.size) sum += data(i)(j)
      print (sum.formatted("%3.2f") + delim)

      for (j <- 0 until categories.size) {
        val proportion = data(i)(j) / sum
        print (proportion.formatted("%1.3f") + delim)
      }


      println ("")
    }


  }

  def calculateKnowledgeDist(in:Array[MCExplQuestion], tablestore:TableStore):Counter[String] = {

    // Step 1: For a given slice of questions, collect a list of all the UIDs in that slice
    val UIDs = scala.collection.mutable.Set[String]()
    for (i <- 0 until in.size) {
      val explQuestion = in(i)
      val expl = explQuestion.expl
      for (j <- 0 until expl.size) {
        val uid = expl(j).uid
        val role = expl(j).role

        //if (role == "BACKGROUND") {      //## Test
          UIDs += uid
        //}

      }

    }

    // Step 2: Calculate the distribution of knowledge in those tables
    val dist = new Counter[String]
    for (uid <- UIDs) {
      val tableIdx = tablestore.UIDtoTableLUT(uid)
      if (tableIdx >= 0) {
        // Get table name
        val tableName = tablestore.tables(tableIdx).name

        // Get knowledge type associated with that table
        val knowledgeType = tableNameToKnowledgeCategory(tableName)
        dist.incrementCount(knowledgeType, 1.0)

      } else {
        //println ("ERROR: UID not found: " + uid)
      }

    }

    // Step 3: Display
    //println ("calculateKnowledgeGrowth")
    //print ( dist.sorted(descending = true) )

    // Return
    dist

  }


  def tableNameToKnowledgeCategory(name:String):String = {
    val complex = Array("PROTO-IF-THEN", "CAUSE", "CHANGE", "CHANGE-VEC", "TRANSFER", "COUPLEDRELATIONSHIP", "PROCESSROLES")
    val supporting = Array("PROTO-ACTION", "USEDFOR", "REQUIRES", "SOURCEOF", "AFFECT", "PROTO-OPPOSITES", "PROTO-FORMEDBY", "AFFORDANCES", "PROTO-DURING", "PROTO-WAVES")

    if (complex.contains(name))     return "COMPLEX"
    if (supporting.contains(name))  return "INFSUPP"

    // Otherwise
    "RET"
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



    // Step 4: Perform monte carlo knowledge growth analysis

    // Step 4A: Read in simulation properties
    // number of monte carlo simulations
    val numSimulations:Int = StringUtils.getInt(props, "skg.numSimulations", 100)

    // Resolution that the analyis will be rendered at (in questions)
    val qStepSize = StringUtils.getInt(props, "skg.qStepSize", 50)



    // Step 4B: Perform analysis
    calculateSummaryStatistics(filteredQuestions)

    calculateTableUseSummary(filteredQuestions, tablestore)

    //calculateKnowledgeGrowthMonteCarlo(filteredQuestions, tablestore, numSimulations = 64000, stepSize = 50)
    calculateKnowledgeGrowthMonteCarlo(filteredQuestions, tablestore, numSimulations = numSimulations, stepSize = qStepSize)

  }


}
