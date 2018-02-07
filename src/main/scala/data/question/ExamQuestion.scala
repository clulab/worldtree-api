package data.question

import edu.arizona.sista.struct.Counter

/**
 * Storage class for a multiple choice science exam question
 * User: peter
 * Date: 1/13/14
 */
class ExamQuestion(val qID:String,
                      val originalID:String,
                      val totalPoints:Int,
                      val correctAnswer:Int,
                      val isMultipleChoice:Boolean,
                      val hasDiagram:Boolean,
                      val examName:String,
                      val schoolGrade:String,
                      val year:String,
                      val questionType:String,
                      val topics:String,
                      val explanation:String,
                      val text:String,
                      val choices:Array[String],
                      val flags:Counter[String] = new Counter[String],
                      val explanationAnnotators:String = "",
                      val questionSimplified:String = "",
                      val questionSimplifiedConc:String = "",
                      val questionFocusPrimary:Array[String] = Array.empty[String],
                      val questionFocusSecondary:Array[String] = Array.empty[String],
                      val answerFocus:Array[Array[String]] = Array.empty[Array[String]] ) {


  // Legacy (for old exam question parser)
  def toMCQuestion:MCQuestionRaw = {
    new MCQuestionRaw(text, questionType, explanation, schoolGrade, topics, qID, correctAnswer, choices)
  }

  /*
   * Supporting functions
   */

  override def toString:String = {
    val os = new StringBuilder

    os.append ("qID:" + qID)
    os.append (" \tOriginalID:" + originalID)
    os.append (" \texam:" + examName)
    os.append (" \tyear:" + year)
    os.append (" \tgrade:" + schoolGrade)
    os.append (" \tisMC:" + isMultipleChoice)
    os.append (" \thasDiagram:" + hasDiagram)
    os.append (" \tqType:" + questionType)
    os.append (" \tTopic(s):" + topics)
    os.append ("\r\n")
    os.append ("Question: " + text + "\r\n")
    if (isMultipleChoice) {
      for (i <- 0 until choices.size) {
        os.append ("mcAnswer[" + i + "]: " + choices(i) + "\t")
      }
      for (i <- 0 until choices.size) {
        os.append ("mcAnswer[" + i + "] Focus: " + answerFocus(i).mkString(", ") + "\t")
      }
      os.append ("\r\n")
    }
    os.append ("Correct Answer: " + correctAnswer + "\r\n")
    os.append (" \r\n")
    os.append (" \tExplanation:" + explanation + "\r\n")
    os.append (" \tExplanation Annotators: " + explanationAnnotators + "\r\n")
    os.append (" \tFlags: " + flags + "\r\n")
    os.append ("Question Focus Words (Primary): " + questionFocusPrimary.mkString(", ") + "\r\n")
    os.append ("Question Focus Words (Secondary): " + questionFocusSecondary.mkString(", ") + "\r\n")
    os.append ("Question (simplified): " + questionSimplified + "\r\n")
    os.append ("Question (simplified, conceptual): " + questionSimplifiedConc)


    os.toString
  }

}
