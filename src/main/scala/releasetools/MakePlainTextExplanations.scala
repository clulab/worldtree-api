package releasetools

import java.io.PrintWriter

import data.question.{ExamQuestionParserDynamic, MCExplQuestion}
import explanationexperiments.CharacterizeConnectivity.{convertToExplQuestions, filterQuestionsByFlags}
import explanationgraph.TableStore

/**
  * This quick tool will make a human-readable list of questions and tablestore explanations, suitable for easy
  * distribution and review.
  * Created by peter on 9/20/17.
  */

object MakePlainTextExplanations {

  def explanationToString(explQuestion:MCExplQuestion, tablestore:TableStore, qid:Int, filterQuestionsAbove:Int = -1):String = {
    val os = new StringBuilder

    val question = explQuestion.question
    val expl = explQuestion.expl

    if (filterQuestionsAbove == -1) {
      os.append(question.toStringMinimal + "\n")
    } else {
      if (qid >= filterQuestionsAbove) {
        os.append("Question: Text of licensed questions can be requested at: http://data.allenai.org/ai2-science-questions-mercury\n")
      } else {
        os.append(question.toStringMinimal + "\n")
      }
    }
    os.append( "Correct Answer: " + question.correctAnswer + "\n" )
    os.append( "Explanation: \n" )
    for (explRow <- expl) {
      val uid = explRow.uid
      val role = explRow.role
      val tablerow = tablestore.getRowByUID(uid)

      os.append(tablerow.toStringSentWithUID() + " (ROLE: " + role + ")\n")
    }

    os.toString()
  }


  def main(args: Array[String]): Unit = {
    var pathAnnotation = "annotation/expl-tablestore-export-2017-08-25-230344/"

    // Step 3: Load tablestore
    val tablestore = new TableStore(pathAnnotation + "tableindex.txt")
    println ( tablestore.tables( tablestore.UIDtoTableLUT("a5c9-d7a4-8421-bb2e") ).name )
    println ( tablestore.getRowByUID("a5c9-d7a4-8421-bb2e") )


    // Step 4: Load questions
    val filenameQuestions = pathAnnotation + "questions.tsv"
    var questions = ExamQuestionParserDynamic.loadQuestionsFromCSVList( filenameQuestions, fullAnnotation = false, noAnnotation = true, tsvMode = true)
    var explQuestions = convertToExplQuestions(questions)
    println ("Loaded " + explQuestions.size + " questions. ")

    // Step 5: Generate plain text output
    val filenameOut = "explanations_plaintext.withmercury.txt"
    val pw = new PrintWriter(filenameOut)

    for (i <- 0 until explQuestions.size) {
      if (explQuestions(i).question.flags.contains("SUCCESS")) {
        pw.println ("Question: " + i)
        //pw.println ( explanationToString(explQuestions(i), tablestore, i, 767) )
        pw.println ( explanationToString(explQuestions(i), tablestore, i) )
        pw.println ("")
      }
    }

    pw.close()

  }

}