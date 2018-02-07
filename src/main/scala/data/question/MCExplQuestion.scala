package data.question

import scala.collection.mutable.ArrayBuffer

/**
  * Storage class for an MCQuestion and a tablestore explanation
  * Created by peter on 7/9/17.
  */
class MCExplQuestion(val question:MCQuestion) {
  val expl = new ArrayBuffer[ExplanationRow]

  /*
   * Constructor
   */
  parseExplanationString( question.explanation )


  /*
   * Explanation String Parsing
   */
  def parseExplanationString(in:String): Unit = {
    if (in.length < 1) return

    var uidTuples = in.trim().toUpperCase.split(" ")
    for (uidTuple <- uidTuples) {
      val fields = uidTuple.split("\\|")
      val uid = fields(0).toLowerCase
      val role = fields(1)

      expl.append( new ExplanationRow(uid, role) )
    }
  }


  /*
   * String methods
   */
  override def toString():String = {
    val os = new StringBuilder
    os.append( question.toString() + "\n" )

    os.append("Explanation (Table Rows):\n")
    for (i <- 0 until expl.size) {
      os.append("\t" + expl(i).uid + " \t" + expl(i).role + "\n")
    }

    os.toString()
  }

}


// Storage class
class ExplanationRow(val uid:String, val role:String) {

  // Determine equality (based on UID, and not role)
  override def equals(that:Any):Boolean = {
    that match {
      case that:ExplanationRow => if (this.uid == that.uid) true else false
      case _ => false
    }
  }


}