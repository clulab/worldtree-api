package explanationexperiments

import data.question.{ExamQuestionParserDynamic, MCExplQuestion, MCQuestion}
import edu.arizona.sista.utils.StringUtils

import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._

/**
  * Monte-carlo simulation to characterize the proportion of questions whose explanations overlap by 1 or more, 2 or more, 3 or more, ..., explanation sentences.
  * The simulation also measures the average cluster size of questions with a given level of connectivity (footnote 6).
  * This simulation generates the data for Figure 5 and footnote 6 in the LREC2018 paper.
  * Created by peter on 7/9/17.
  */

object CharacterizeConnectivity {
  val VERSION = "1.0"
  val PROGRAM_TITLE = "WorldTree: CharacterizeConnectivity version " + VERSION


  // Reporting modes
  val MODE_MAX  = 0     // Report as maximum connectivity a given question recieves (distribution sums to 100%)
  val MODE_INCL = 1     // Report is all questions with connectivity at a given level (distribution does not sum to 100%)

  // Overlap/Connectivity modes
  val MODE_ANY              = 0     // Allow connections on any explanation roles
  val MODE_CENTRAL_ONLY     = 1     // Only count connections/overlap that are entirely made out of CENTRAL roles (at least one question labeling a given row as CENTRAL)
  val MODE_CENTRAL_ATLEAST1 = 2     // Only count connections where AT LEAST 1 of the overlapping explanation sentences is labeled as central

  /*
   * Determine connectivity (monte-carlo simulation)
   */
  def determineExplOverlapMonteCarlo(in:Array[MCExplQuestion], steps:Int = 25, numSimulations:Int = 10, maxHistBins:Int = 6, modeOverlap:Int=MODE_ANY, modeReport:Int=MODE_MAX) = {
    //var maxHistBins:Int = 6   // 0 connectivity, 1, 2, 3, 4, 5+ sentences/rows in common
    var curNumQuestions:Int = steps
    if (curNumQuestions > in.size) curNumQuestions = in.size-1

    val data = new ArrayBuffer[Array[Double]]
    val dataAvgConnectivity = new ArrayBuffer[Array[Double]]
    val stepSizes = new ArrayBuffer[Int]

    // For each simulation size (a given number of questions)
    while (curNumQuestions < in.size) {
      println ("curNumQuestions: " + curNumQuestions + " / " + in.size)

      val hist = Array.fill[Double](maxHistBins)(0.0)
      val avgConnectivity = Array.fill[Double](maxHistBins)(0.0)

      // If the number of questions is low, increase the number of samples to get a better estimate (without sacrificing too much simulation time)
      var numSimulationsAdj:Int = numSimulations
      if (curNumQuestions < 500) numSimulationsAdj = numSimulationsAdj * 4

      for (i <- 0 until numSimulationsAdj) {
        // Step 1: Subsample questions
        val subsampledQuestions = subsampleQuestions(in, curNumQuestions)

        // Step 2: Determine minimum overlap distribution in this set
        var histSim:Array[Double] = null
        var avgConnectivitySim:Array[Double] = null
        if (modeReport == MODE_MAX) {
          // Maximum connectivity of each question -- here the sum of the distribution will be 100%
          val (histSim1, avgConnectivitySim1) = determineMaxExplOverlap(subsampledQuestions, maxBins = maxHistBins, modeOverlap)
          histSim = histSim1
          avgConnectivitySim = avgConnectivitySim1
        } else if (modeReport == MODE_INCL) {
          // Inclusive connectivity of each question -- if a question has connectivity up to (e.g.) 4 rows, it will count for rows 1, 2, 3, and 4
          val (histSim1, avgConnectivitySim1) = determineInclExplOverlap(subsampledQuestions, maxBins = maxHistBins, modeOverlap)
          histSim = histSim1
          avgConnectivitySim = avgConnectivitySim1
        }

        // Step 3: Add to overall distribution
        for (j <- 0 until histSim.size) {
          hist(j) += (histSim(j) / numSimulationsAdj)
          avgConnectivity(j) += (avgConnectivitySim(j) / numSimulationsAdj)
        }
      }

      // Step 4: Store data
      data.append(hist)
      dataAvgConnectivity.append(avgConnectivity)
      stepSizes.append(curNumQuestions)

      curNumQuestions += steps
    }


    // Display
    println ("Connectivity Analysis:")
    if (modeReport == MODE_MAX)                 println ("Report Mode: Maximum connectivity mode")
    if (modeReport == MODE_INCL)                println ("Report Mode: Inclusive connectivity mode")

    if (modeOverlap == MODE_ANY)                println ("Overlap Mode: Any roles")
    if (modeOverlap == MODE_CENTRAL_ONLY)       println ("Overlap Mode: Only CENTRAL roles")
    if (modeOverlap == MODE_CENTRAL_ATLEAST1)   println ("Overlap Mode: At least 1 CENTRAL role")

    println ("numSimulations: " + numSimulations)
    println ("")

    // Human readable
    for (i <- 0 until stepSizes.size) {
      println ("Step size: " + stepSizes(i))
      val hist = data(i)
      val avgConnectivity = dataAvgConnectivity(i)
      for (j <- 0 until hist.size) {
        println("\tBIN " + j + ": " + hist(j).formatted("%3.3f").formatted("%10s") + ("(" + (100*hist(j)/stepSizes(i).toDouble).formatted("%3.1f") + "%)").formatted("%10s") + "\t" + avgConnectivity(j).formatted("%3.3f") )
      }
      println ("")
    }
    println ("")

    // Delimited
    val delim = "\t"
    println("Delimited tables: ")
    println("")

    println("Proportion of questions with a given level of overlap:")
    println("")
    print("numQ" + delim)
    for (j <- 0 until maxHistBins) {
      print( "BIN_" + j + delim )
    }
    println("")

    for (i <- 0 until stepSizes.size) {
      print (stepSizes(i) + "\t")
      val hist = data(i)
      val avgConnectivity = dataAvgConnectivity(i)
      for (j <- 0 until hist.size) {
        print( (hist(j)/stepSizes(i).toDouble).formatted("%3.3f") + delim )
      }
      println ("")
    }
    println ("")
    println ("")

    println("Average cluster sizes:")
    println("(i.e. average number of questions a given question connects to, for a given level of overlap)")
    println("")
    print("numQ" + delim)
    for (j <- 0 until maxHistBins) {
      print( "BIN_" + j + delim )
    }
    println("")

    for (i <- 0 until stepSizes.size) {
      print (stepSizes(i) + "\t")
      val hist = data(i)
      val avgConnectivity = dataAvgConnectivity(i)
      for (j <- 0 until hist.size) {
        print( avgConnectivity(j).formatted("%3.2f") + delim )
      }
      println ("")
    }

  }


  /*
   * Determine connectivity (maximum/exclusive, on a given set of questions)
   */

  // Determine the maximum explanation overlap for all questions in a given set.  Returns a histogram, and the average number of questions connected at each level of connectivity.
  def determineMaxExplOverlap(in:Array[MCExplQuestion], maxBins:Int = 6, modeOverlap:Int = MODE_ANY):(Array[Double], Array[Double]) = {
    val hist = Array.fill[Double](maxBins)(0.0)
    val avgConnectivity = Array.fill[Double](maxBins)(0.0)

    // For each question, determine that question's minimum explanation overlap (in terms of numbers of rows)
    for (qIdx <- 0 until in.size) {
      var (maxOverlap, numConnections) = determineMaxExplOverlapQuestion(qIdx, in, modeOverlap)
      if (maxOverlap >= maxBins) maxOverlap = maxBins-1
      hist(maxOverlap) += 1
      avgConnectivity(maxOverlap) += numConnections
    }

    // Take average for average connectivity
    for (i <- 0 until avgConnectivity.size) {
      if (hist(i) > 0) {      // Prevent divide-by-zero errors where no connectivity was found
        avgConnectivity(i) = avgConnectivity(i) / hist(i).toDouble
      }
    }

    // Return
    (hist, avgConnectivity)
  }

  // Helper function: Determine the maximum explanation overlap for a specific question in the input array 'in'
  // Returns both the number of sentences that overlap with at least one other question, as well as how many
  //    other questions have this level of amount of overlap with that question.
  def determineMaxExplOverlapQuestion(qIdx:Int, in:Array[MCExplQuestion], modeOverlap:Int = MODE_ANY):(Int, Int) = {
    var maxOverlap:Int = 0
    var numConnectionsAtMaxOverlap:Int = 0

    // Step 1: Retrieve query explanation
    val explQuery = in(qIdx).expl

    for (i <- 0 until in.size) {
      var numOverlap:Int = 0
      var numOverlapCentral:Int = 0

      if (i != qIdx) {
        // Determine how many rows each explanation share in common
        val explCompare = in(i).expl
        for (j <- 0 until explQuery.size) {
          breakable {
            for (k <- 0 until explCompare.size) {
              if (explQuery(j) == explCompare(k)) {
                numOverlap += 1
                if ((explQuery(j).role == "CENTRAL") || (explCompare(k).role == "CENTRAL")) {   // Only count explanation sentences labeled "CENTRAL", if allRoles=false
                  numOverlapCentral += 1
                }
                break()
              }
            }
          }
        }

        // Store maximum overlap value
        if (modeOverlap == MODE_ANY) {
          // Any connections count
          if (numOverlap == maxOverlap)  {
            numConnectionsAtMaxOverlap += 1
          } else if (numOverlap > maxOverlap) {
            maxOverlap = numOverlap
            numConnectionsAtMaxOverlap = 1        // Start at one, because this question the query question is being compared to also counts
          }

        } else if (modeOverlap == MODE_CENTRAL_ONLY) {
          // Only CENTRAL connections count
          if (numOverlapCentral == maxOverlap)  {
            numConnectionsAtMaxOverlap += 1
          } else if (numOverlapCentral > maxOverlap) {
            maxOverlap = numOverlapCentral
            numConnectionsAtMaxOverlap = 1        // Start at one, because this question the query question is being compared to also counts
          }


        } else if (modeOverlap == MODE_CENTRAL_ATLEAST1) {
          // As long as there is at least one CENTRAL count, then all connections count
          if (numOverlapCentral >= 1) {
            if (numOverlap == maxOverlap) {
              numConnectionsAtMaxOverlap += 1
            } else if (numOverlap > maxOverlap) {
              maxOverlap = numOverlap
              numConnectionsAtMaxOverlap = 1 // Start at one, because this question the query question is being compared to also counts
            }
          }

        }

      }
    }

    // Return
    (maxOverlap, numConnectionsAtMaxOverlap)
  }


  /*
   * Determine connectivity (inclusive, on a given set of questions)
   */

  // Determine the explanation overlap distribution for all questions in a given set.  Returns a histogram, and the average number of questions connected at each level of connectivity.
  def determineInclExplOverlap(in:Array[MCExplQuestion], maxBins:Int = 6, modeOverlap:Int=MODE_ANY):(Array[Double], Array[Double]) = {
    val hist = Array.fill[Double](maxBins)(0.0)
    val avgConnectivity = Array.fill[Double](maxBins)(0.0)

    // For each question, determine that question's minimum explanation overlap (in terms of numbers of rows)
    for (qIdx <- 0 until in.size) {
      val (boolOverlap, avgConnectivitityQ) = determineInclExplOverlapQuestion(qIdx, in, maxBins, modeOverlap)

      for (i <- 0 until maxBins) {
        hist(i) += boolOverlap(i)
        avgConnectivity(i) += avgConnectivitityQ(i)
      }
    }

    // Take average for average connectivity
    for (i <- 0 until avgConnectivity.size) {
      if (hist(i) > 0) { // Prevent divide-by-zero errors where no connectivity was found
        avgConnectivity(i) = avgConnectivity(i) / hist(i).toDouble
      }
    }

    // Return
    (hist, avgConnectivity)
  }

  // Helper function: Determine the explanation overlap distribution for a specific question in the input array 'in'
  def determineInclExplOverlapQuestion(qIdx:Int, in:Array[MCExplQuestion], maxBins:Int = 6, modeOverlap:Int = MODE_ANY):(Array[Double], Array[Double]) = {
    val boolOverlap = Array.fill[Double](maxBins)(0.0)           // Does the question have at least one connection at a given level of connectivity?
    val numConnectivity = Array.fill[Double](maxBins)(0.0)       // Number of questions with a given level of connectivity


    // Step 1: Retrieve query explanation
    val explQuery = in(qIdx).expl

    for (i <- 0 until in.size) {
      var numOverlap:Int = 0
      var numOverlapCentral:Int = 0

      if (i != qIdx) {
        // Determine how many rows each explanation share in common
        val explCompare = in(i).expl
        for (j <- 0 until explQuery.size) {
          breakable {
            for (k <- 0 until explCompare.size) {
              if (explQuery(j) == explCompare(k)) {
                numOverlap += 1
                if ((explQuery(j).role == "CENTRAL") || (explCompare(k).role == "CENTRAL")) {   // Only count explanation sentences labeled "CENTRAL", if allRoles=false
                  numOverlapCentral += 1
                }
                break()
              }
            }
          }
        }

        // Bound checking
        if (numOverlap >= maxBins) numOverlap = maxBins-1
        if (numOverlapCentral >= maxBins) numOverlapCentral = maxBins-1

        if (modeOverlap == MODE_ANY) {
          for (dec <- 0 to numOverlap) {
            if (modeOverlap == MODE_ANY) {
              // Any connections count
              boolOverlap(numOverlap - dec) = 1 // Store binary "did this question have this level of connectivity with at least one other question" measure
              numConnectivity(numOverlap - dec) += 1
            }
          }

        } else if ((modeOverlap == MODE_CENTRAL_ONLY) || (modeOverlap == MODE_CENTRAL_ATLEAST1)) {

          for (dec <- 0 to numOverlapCentral) {
            if (modeOverlap == MODE_CENTRAL_ONLY) {
              // Only CENTRAL connections count
              boolOverlap(numOverlapCentral - dec) = 1 // Store binary "did this question have this level of connectivity with at least one other question" measure
              numConnectivity(numOverlapCentral - dec) += 1

            } else if (modeOverlap == MODE_CENTRAL_ATLEAST1) {
              // As long as there is at least one CENTRAL count, then all connections count
              if (numOverlapCentral-dec >= 1) {
                boolOverlap(numOverlap - dec) = 1 // Store binary "did this question have this level of connectivity with at least one other question" measure
                numConnectivity(numOverlap - dec) += 1
              } else {
                boolOverlap(0) = 1 // Store binary "did this question have this level of connectivity with at least one other question" measure
                numConnectivity(0) += 1
              }
            }
          }

        }

      }
    }

    // Return
    (boolOverlap, numConnectivity)
  }




  /*
   * Supporting functions
   */

  def convertToExplQuestions(in:Array[MCQuestion]):Array[MCExplQuestion] = {
    val out = new ArrayBuffer[MCExplQuestion]
    for (i <- 0 until in.size) {
      out.append( new MCExplQuestion(in(i)) )
    }
    // Return
    out.toArray
  }

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

  // Randomly retrieve a subsample of questions
  def subsampleQuestions(in:Array[MCExplQuestion], numQuestions:Int):Array[MCExplQuestion] = {
    val in1 = new ArrayBuffer[MCExplQuestion]
    in1.insertAll(0, in)
    val shuffled = scala.util.Random.shuffle( in1 )
    // Return
    shuffled.slice(0, numQuestions).toArray
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
    //val filteredQuestions = filterQuestionsByFlags(explQuestions, Array("SUCCESS") )

    // Step 3: Perform monte carlo connectivity analysis

    // Step 3A: Read in simulation properties
    var overlapMode:Int = -1
    if (props.getProperty("cc.overlapMode", "") != "") {
      if (props.getProperty("cc.overlapMode", "").toUpperCase == "MODE_CENTRAL_ONLY") {
        overlapMode = MODE_CENTRAL_ONLY
      } else if (props.getProperty("cc.overlapMode", "").toUpperCase == "MODE_CENTRAL_ATLEAST1") {
        overlapMode = MODE_CENTRAL_ATLEAST1
      } else if (props.getProperty("cc.overlapMode", "").toUpperCase == "MODE_ANY") {
        overlapMode = MODE_ANY
      } else {
        throw new RuntimeException("ERROR: 'cc.overlapMode' property not a recognized value (MODE_CENTRAL_ONLY, MODE_CENTRAL_ATLEAST1, MODE_ANY).")
      }
    } else {
      throw new RuntimeException("ERROR: Unable to find 'cc.overlapMode' property in properties file.")
    }

    // number of monte carlo simulations
    val numSimulations:Int = StringUtils.getInt(props, "cc.numSimulations", 100)

    // maxHistBins: 0 connectivity, 1, 2, 3, 4, 5+ sentences/rows in common
    val maxHistBins:Int = StringUtils.getInt(props, "cc.maxHistBins", 6)

    // Resolution that the analyis will be rendered at (in questions)
    val qStepSize = StringUtils.getInt(props, "cc.qStepSize", 50)


    // Step 3B: Perform analysis
    //determineExplOverlapMonteCarlo(filteredQuestions, steps = 50, numSimulations, maxHistBins, overlapMode, modeReport = MODE_MAX)
    //println ("")
    println ("Beginning Monte-carlo Simulation... ")
    determineExplOverlapMonteCarlo(filteredQuestions, steps = qStepSize, numSimulations, maxHistBins, overlapMode, modeReport = MODE_INCL)


  }

}
