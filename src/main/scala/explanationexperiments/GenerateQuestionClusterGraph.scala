package explanationexperiments

import java.io.PrintWriter

import data.question.{ExamQuestionParserDynamic, MCExplQuestion, MCQuestion}
import edu.arizona.sista.utils.StringUtils
import explanationexperiments.CharacterizeConnectivity._

import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks.{break, breakable}

/**
  * Exports the question connectivity graph into a DOT/Graphviz format, for external rendering.
  * Gephi (http://www.gephi.org) is particularly useful for visualizing these large graphs, and a slightly modified
  * version of Gephi was used to create the Worldtree corpus visualzation included in the paper (Figure 4) and release.
  * Created by peter on 7/11/17.
  */

object GenerateQuestionClusterGraph {
  val VERSION = "1.0"
  val PROGRAM_TITLE = "WorldTree: GenerateQuestionClusterGraph version " + VERSION

  /*
 * Determine connectivity (monte-carlo simulation)
 */
  def generateExplConnectivityGraph(filenameOutPrefix:String, in:Array[MCExplQuestion], minConnectivity:Int=1, allRoles:Boolean = true) {
    val osNodes = new StringBuilder()
    val osLinks = new StringBuilder()

    println (" * generateExplConnectivityGraph: started...")

    // Step 1: Determine links
    val nodeUsed = Array.fill[Boolean](in.size)(false)
    val existingLinks = Array.ofDim[Boolean](in.size,in.size)

    // Initialize array of links
    for (i <- 0 until in.size) {
      for (j <- 0 until in.size) {
        existingLinks(i)(j) = false
      }
    }


    for (i <- 0 until in.size) {
      val (linkStr, nodesUsed) = determineExplOverlapQuestion(i, in, existingLinks, minConnectivity, allRoles)

      // Add link text
      osLinks.append(linkStr)

      // Note which nodes have been used
      for (nodeIdx <- nodesUsed) {
        nodeUsed(nodeIdx) = true
      }
    }

    // Step 2: Generate nodes
    for (i <- 0 until nodeUsed.size) {
      if (nodeUsed(i)) {
        // Export node for this question
        val question = in(i).question
        val questionText = wordWrapStrDOT( question.text, maxLengthLine = 25 ).replaceAll("\"", "'")
        osNodes.append("\tnode" + i + " [label=\"" + questionText + "\"]\n")
      }
    }

    // Display summary statistics
    println (" * generateExplConnectivityGraph: number of links: " + osLinks.count(_ == '\n'))
    println (" * generateExplConnectivityGraph: number of nodes: " + osNodes.count(_ == '\n'))

    // Step 3: Export
    val filenameOut = filenameOutPrefix + "_q" + in.size + "_minC" + minConnectivity + "_allRoles" + allRoles + ".dot"
    println (" * generateExplConnectivityGraph: exporting DOT graph (filename = " + filenameOut + ")")
    val pw = new PrintWriter(filenameOut)

    pw.println("graph G {")
    pw.println("\toverlap=false;")
    pw.println("\tsep=\"+25,25\";")
    pw.print( osLinks )
    pw.print( osNodes )
    pw.println("}")

    pw.close()

    println (" * generateExplConnectivityGraph: complete...")

  }


  /*
   * Determine explanation overlap
   */

  def determineExplOverlapQuestion(qIdx:Int, in:Array[MCExplQuestion], existingLinks:Array[Array[Boolean]], minConnectivity:Int = 1, allRoles:Boolean = true):(String, Array[Int]) = {
    val os = new StringBuilder
    val nodesUsed = new ArrayBuffer[Int]

    // Step 1: Retrieve query explanation
    val explQuery = in(qIdx).expl

    for (i <- 0 until in.size) {
      var numOverlap:Int = 0
      var numOverlapConceptual:Int = 0

      if (i != qIdx) {
        // Determine how many rows each explanation share in common
        val explCompare = in(i).expl
        for (j <- 0 until explQuery.size) {
          breakable {
            for (k <- 0 until explCompare.size) {
              if (explQuery(j) == explCompare(k)) {
                numOverlap += 1
                if ((explQuery(j).role == "CENTRAL") || (explCompare(k).role == "CENTRAL")) {
                  numOverlapConceptual += 1
                }
                break()
              }
            }
          }
        }

        if ((numOverlap >= minConnectivity) && (existingLinks(qIdx)(i) == false)) {
          if ((allRoles == true) || (numOverlapConceptual > 0)) {
            os.append("\tnode" + qIdx + " -- node" + i + " [label=\"" + numOverlap + "\",weight=" + numOverlap + "]\n")
            if (nodesUsed.size == 0) nodesUsed.append(qIdx)
            // Note node usage
            nodesUsed.append(i)
            // Note link
            existingLinks(qIdx)(i) = true
            existingLinks(i)(qIdx) = true
          }
        }
      }
    }

    // Return
    (os.toString, nodesUsed.toArray)
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


  // Breaks a string into a number of lines, each of approximately maxLength (allowing for long words to exceed this limit)
  def wordWrapStrDOT(in:String, maxLengthLine:Int = 25, maxTotalLength:Int = 150):String = {
    val os = new StringBuilder
    val words = in.split(" ")
    var lenCount:Int = 0

    breakable {
      for (i <- 0 until words.size) {
        if (os.length >= maxTotalLength) {
          os.append("...")
          break()
        }

        if (lenCount + words(i).length <= maxLengthLine) {
          os.append(words(i) + " ")
          lenCount += words(i).length
        } else if (lenCount == 0) {
          // One really long word -- add to it's own line
          os.append(words(i) + "<br>")
          lenCount == 0
        } else {
          //os.append(" \\n")   // The extra space at the front is so the text renders okay in Gephi
          os.append("<br>" + words(i) + " ")   // The extra space at the front is so the text renders okay in Gephi
          lenCount = words(i).length
        }
      }
    }

    // Return
    os.toString()
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
    println ("Number of questions after filtering: " + filteredQuestions.size)
    println ("")

    // Step 3: Generate and export graph

    // Step 3A: Read in generation properties
    val filenameOut = props.getProperty("gqcg.filenameOut", "")
    if (filenameOut == "") {
      throw new RuntimeException("ERROR: Unable to find 'gqcg.filenameOut' property in properties file.")
    }

    // Minimum number of overlaping explanation sentences to draw an edge
    val minConnectivity = StringUtils.getInt(props, "gqcg.minConnectivity", 1)

    // Allow connections on all roles (true = central, grounding, background, lexical glue), or just central (false)
    val allRoles = StringUtils.getBool(props, "gqcg.allRoles", default = false)

    // Step 3B: Generate and export graph
    generateExplConnectivityGraph(filenameOut, filteredQuestions, minConnectivity = minConnectivity, allRoles = allRoles)

  }



}