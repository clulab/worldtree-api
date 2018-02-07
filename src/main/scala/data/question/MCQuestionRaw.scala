package data.question

import edu.arizona.sista.processors.Document
import edu.arizona.sista.struct.Counter


/**
 * Storage classes for a generic multiple choice question
 * User: peter
 * Date: 1/13/14
 */

// Storage class for one multiple choice question (not including annotation)
class MCQuestionRaw(val text:String,
                    val qType:String,
                    val justification:String,
                    val grade:String,
                    val topics:String,
                    val questionID:String,
                    val correctAnswer:Int,
                    val choices:Array[String]) {


  override def toString:String = {
    val os = new StringBuilder

    os.append ("Question: " + text + "\r\n")
    for (i <- 0 until choices.size) {
      os.append ("mcAnswer[" + i + "]: " + choices(i) + "\t")
    }
    os.append ("\r\n")
    os.append ("Correct Answer: " + correctAnswer + "\r\n")
    os.append ("Quesiton Type: " + qType + "\r\n")
    os.append ("Justification: " + justification + "\r\n")
    os.append ("Grade Level: " + grade + "\r\n")
    os.append ("Topics: " + topics + "\r\n")
    os.append ("Question ID: " + questionID + "\r\n")

    os.toString
  }

}

// Storage class for one multiple choice question including annotation and answers
class MCQuestion(val text:String,
                 val annotation:Document,
                 val qType:String,
                 val explanation:String,
                 val explanationAnnotation:Document,
                 val grade:String,
                 val topic:Array[String],
                 val questionID:String,
                 val correctAnswer:Int,
                 val choices:Array[MCAnswer],
                 val flags:Counter[String] = new Counter[String],
                 val explanationAnnotators:String = "",
                 val hasFocusWords:Boolean = false,
                 var focusQuestionPrimary:Array[String] = Array.empty[String],
                 var focusQuestionSecondary:Array[String] = Array.empty[String],
                 val textSimplified:Option[String] = None,
                 val textSimplifiedConc:Option[String] = None) {



  /*
   * Test for equality (looking only at question text content, and correct answer index, not other metadata)
   */
  override def equals(that:Any):Boolean = {
    that match {
      case that:MCQuestion => {
        compareTextOnly( that.asInstanceOf[MCQuestion] )
      }
      case _ => false
    }
  }

  // Compares two questions for equality looking only at their content text and correct answer, not other metadata
  def compareTextOnly(that:MCQuestion):Boolean = {
    if (this.correctAnswer != that.correctAnswer) return false
    if (this.text != that.text) return false

    if (this.choices.size != that.choices.size) return false
    for (i <- 0 until this.choices.size) {
      if (this.choices(i).text != that.choices(i).text) return false
    }

    // Return
    true
  }


  /*
   * Supporting Functions
   */

  override def toString:String = {
    val os = new StringBuilder

    os.append ("Question: " + text + "\r\n")
    for (i <- 0 until choices.size) {
      os.append ("mcAnswer[" + i + "]: " + choices(i).text + "\t")
    }
    os.append ("\r\n")
    os.append ("Correct Answer: " + correctAnswer + "\r\n")
    os.append ("Question type: " + qType + "   ")
    os.append ("Grade: " + grade + "   ")
    os.append ("Topics: " + topic.mkString(",") + "   ")
    os.append ("Question ID: " + questionID)
    os.append ("\r\n")
    os.append ("Gold Explanation: " + explanation + "\r\n")
    os.append ("Explanation Annotators: " + explanationAnnotators + "\r\n")
    os.append ("Flags: " + flags.toShortString + "\r\n")

    os.append ("\n")
    if (hasFocusWords == true) {
      os.append ("Focus Words (Question, Primary): " + focusQuestionPrimary.mkString(", "))
      os.append ("\r\n")
      os.append ("Focus Words (Question, Secondary): " + focusQuestionSecondary.mkString(", "))
      os.append ("\r\n")
      for (i <- 0 until choices.size) {
        os.append("Focus (A" + i + "): " + choices(i).focusAnswer.mkString(",") + " \t")
      }
      os.append ("\r\n")
    } else {
      os.append ("hasFocusWords: false")
    }

    if ((!textSimplified.isEmpty) || (!textSimplifiedConc.isEmpty)) {
      os.append ("Question Text (Simplified): " + textSimplified.getOrElse("Empty"))
      os.append ("\r\n")
      os.append ("Question Text (Simplified, Conceptual): " + textSimplifiedConc.getOrElse("Empty"))
      os.append ("\r\n")
    } else {
      os.append ("Question Text (Simplified): Not present")
      os.append ("\r\n")
    }


    os.toString
  }

  def toStringMinimal:String = {
    val os = new StringBuilder

    os.append ("Question: " + text + "\t")
    for (i <- 0 until choices.size) {
      os.append ("[" + i + "]: " + choices(i).text + "\t")
    }

    os.toString
  }


}

// Storage class for one multiple choice answer including annotation
class MCAnswer(val text:String,
               val annotation:Document,
               val focusAnswer:Array[String] = Array.empty[String]) {

  override def toString:String = text

}