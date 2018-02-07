package data.question

import java.io.File

import collection.mutable.ArrayBuffer
import edu.arizona.sista.utils.StringUtils
import org.slf4j.LoggerFactory
import edu.arizona.sista.processors.{Document, Processor}
import edu.arizona.sista.processors.corenlp.CoreNLPProcessor
import com.github.tototoshi.csv._
import edu.arizona.sista.struct.Counter

import scala.util.control.Breaks._


/**
  * Parser for exam questions in AI2 format
  * This second version of the exam question parser populates fields dynamically based on which columns are included in the question file.
  * This is intended to allow extended annotation (e.g. gold explanations, knowledge type, gold focus words, etc) to be present or not.
  * This parser requires the first line of the question file to be a column header.
  * Created by peter on 1/16/17.
  */
class ExamQuestionParserDynamic {

}

object ExamQuestionParserDynamic {
  lazy val processor:Processor = new CoreNLPProcessor()
  val logger = LoggerFactory.getLogger(classOf[ExamQuestionParserDynamic])

  /*
   Field Names
   */
  // Original
  val LABEL_QUESTIONID        =   "questionID".toUpperCase
  val LABEL_ORIGQUESTIONID    =   "originalQuestionID".toUpperCase
  val LABEL_TOTALPOINTS       =   "totalPossiblePoint".toUpperCase
  val LABEL_CORRECTANSWER     =   "answerKey".toUpperCase
  val LABEL_ISMCQUESTION      =   "isMultipleChoiceQuestion".toUpperCase
  val LABEL_ISDIAGRAM         =   "includesDiagram".toUpperCase
  val LABEL_EXAMNAME          =   "examName".toUpperCase
  val LABEL_GRADE             =   "schoolGrade".toUpperCase
  val LABEL_YEAR              =   "year".toUpperCase
  val LABEL_QUESTIONTEXT      =   "question".toUpperCase
  val LABEL_SUBJECT           =   "subject".toUpperCase
  val LABEL_CATEGORY          =   "category".toUpperCase

  // Extended
  val LABEL_KNOWLEDGETYPE           =   "knowledgeType".toUpperCase
  val LABEL_TOPIC                   =   "topic".toUpperCase
  val LABEL_GOLDEXPLANATION         =   "explanation".toUpperCase
  val LABEL_QUESTIONSIMPLIFIED      =   "QSimplified".toUpperCase
  val LABEL_QUESTIONSIMPLIFIEDCONC  =   "QConceptual".toUpperCase
  val LABEL_QUESTIONFOCUS_PRIMARY   =   "QFocus".toUpperCase
  val LABEL_QUESTIONFOCUS_SECONDARY =   "QSecondary".toUpperCase
  val LABEL_ANSWERFOCUS_ANSA        =   "AFocusA".toUpperCase
  val LABEL_ANSWERFOCUS_ANSB        =   "AFocusB".toUpperCase
  val LABEL_ANSWERFOCUS_ANSC        =   "AFocusC".toUpperCase
  val LABEL_ANSWERFOCUS_ANSD        =   "AFocusD".toUpperCase

  val LABEL_FLAGS                   =   "Flags".toUpperCase
  val LABEL_EXPLANATIONANNOTATORS   =   "ExplanationAnnotators".toUpperCase


  // Read the header line of the CSV file, and create a map that identifies which field/column index each piece of information is at
  def getColumnMapping(headerFields:List[String]):(scala.collection.mutable.Map[String, Int], String) = {
    val out = new scala.collection.mutable.HashMap[String, Int]().withDefaultValue(-1)
    val warningText = new StringBuilder()

    for (fieldIdx <- 0 until headerFields.size) {
      val headerFieldText = headerFields(fieldIdx).toUpperCase

      headerFieldText match {
        case LABEL_QUESTIONID               => out(LABEL_QUESTIONID) = fieldIdx
        case LABEL_ORIGQUESTIONID           => out(LABEL_ORIGQUESTIONID) = fieldIdx
        case LABEL_TOTALPOINTS              => out(LABEL_TOTALPOINTS) = fieldIdx
        case LABEL_CORRECTANSWER            => out(LABEL_CORRECTANSWER) = fieldIdx
        case LABEL_ISMCQUESTION             => out(LABEL_ISMCQUESTION) = fieldIdx
        case LABEL_ISDIAGRAM                => out(LABEL_ISDIAGRAM) = fieldIdx
        case LABEL_EXAMNAME                 => out(LABEL_EXAMNAME) = fieldIdx
        case LABEL_GRADE                    => out(LABEL_GRADE) = fieldIdx
        case LABEL_YEAR                     => out(LABEL_YEAR) = fieldIdx
        case LABEL_QUESTIONTEXT             => out(LABEL_QUESTIONTEXT) = fieldIdx
        case LABEL_SUBJECT                  => out(LABEL_SUBJECT) = fieldIdx
        case LABEL_CATEGORY                 => out(LABEL_CATEGORY) = fieldIdx

        case LABEL_KNOWLEDGETYPE            => out(LABEL_KNOWLEDGETYPE) = fieldIdx
        case LABEL_TOPIC                    => out(LABEL_TOPIC) = fieldIdx
        case LABEL_GOLDEXPLANATION          => out(LABEL_GOLDEXPLANATION) = fieldIdx
        case LABEL_QUESTIONSIMPLIFIED       => out(LABEL_QUESTIONSIMPLIFIED) = fieldIdx
        case LABEL_QUESTIONSIMPLIFIEDCONC   => out(LABEL_QUESTIONSIMPLIFIEDCONC) = fieldIdx
        case LABEL_QUESTIONFOCUS_PRIMARY    => out(LABEL_QUESTIONFOCUS_PRIMARY) = fieldIdx
        case LABEL_QUESTIONFOCUS_SECONDARY  => out(LABEL_QUESTIONFOCUS_SECONDARY) = fieldIdx
        case LABEL_ANSWERFOCUS_ANSA         => out(LABEL_ANSWERFOCUS_ANSA) = fieldIdx
        case LABEL_ANSWERFOCUS_ANSB         => out(LABEL_ANSWERFOCUS_ANSB) = fieldIdx
        case LABEL_ANSWERFOCUS_ANSC         => out(LABEL_ANSWERFOCUS_ANSC) = fieldIdx
        case LABEL_ANSWERFOCUS_ANSD         => out(LABEL_ANSWERFOCUS_ANSD) = fieldIdx

        case LABEL_FLAGS                    => out(LABEL_FLAGS) = fieldIdx
        case LABEL_EXPLANATIONANNOTATORS    => out(LABEL_EXPLANATIONANNOTATORS) = fieldIdx

        // Default
        case _ => warningText.append("WARNING: Found unrecognized header label (" + headerFieldText + ") in question file. \n")
      }

    }

    // Return
    (out, warningText.toString)
  }


  // Main entry point
  def loadQuestionsFromCSVList(qList:String, fullAnnotation:Boolean = true, noAnnotation:Boolean = false, annotateExplanations:Boolean = true, tsvMode:Boolean = false):Array[MCQuestion] = {
    toMCQuestion( parse( qList.split(",").toArray, tsvMode ), fullAnnotation, noAnnotation, annotateExplanations)
  }

  def parse(filenames:Array[String], tsvMode:Boolean = false):Array[ExamQuestion] = {
    // Returns all multiple choice questions from the list of files supplied ("filenames")
    val warningText = new StringBuilder

    // Step 1: Load all questions from supplied files
    val questions = new ArrayBuffer[ExamQuestion]
    for (filename <- filenames) {
      val (questionsFile, warningTextFile) = load(filename, tsvMode)
      questions ++= questionsFile
      warningText.append(warningTextFile)
    }

    logger.info("Found " + questions.size + " questions before filtering. ")

    // Step 2: Filter to only include MC questions that do not contain diagrams
    var i:Int = 0
    while (i < questions.size) {
      if ((questions(i).isMultipleChoice) && (!questions(i).hasDiagram)) {
        i += 1
      } else {
        questions.remove(i)
      }
    }

    // Step 4: Display questions
    /*
    for (question <- questions) {
      logger.debug (question.toString)
      logger.debug ("----")
    }
    */
    val numWarnings = warningText.count(_ == '\n')
    logger.info ("Parsed " + questions.size + " questions after filtering with " + numWarnings + " warnings. ")
    logger.warn (warningText.toString)

    questions.toArray
  }

  // Parses MC Questions in plain text format. e.g. What colour is water? (a) red (b) green (c) blue (d) orange
  // Returns tuple of (question text, Array[Answer Text])
  def textToMC(text:String):(String, Array[String]) = {
    val qsplit = text.split("\\(([A-D]|[a-d]|[1-4])\\)")
    val questionText = qsplit(0).trim
    val mcAnswers = new ArrayBuffer[String]
    for (i <- 1 until qsplit.size) {
      mcAnswers.append(qsplit(i).trim)
    }
    (questionText, mcAnswers.toArray)
  }

  // Convert from a ExamQuestion back to the AI2 Question file format
  def MCToText(in:ExamQuestion):String = {
    val os = new StringBuilder
    os.append(in.qID + "\t")
    os.append(in.originalID + "\t")
    os.append(in.totalPoints + "\t")
    os.append((65 + in.correctAnswer).toChar + "\t")

    if (in.isMultipleChoice) {
      os.append("1\t")
    } else {
      os.append("0\t")
    }
    if (in.hasDiagram) {
      os.append("1\t")
    } else {
      os.append("0\t")
    }

    os.append(in.examName + "\t")
    os.append(in.schoolGrade + "\t")
    os.append(in.year + "\t")
    os.append(in.questionType + "\t")
    os.append(in.text + "    ")

    for (aIdx <- 0 until in.choices.size) {
      val letter:Char = (65 + aIdx).toChar
      os.append(" (" + letter + ") " + in.choices(aIdx))
    }

    os.toString()
  }

  // Convert from letter indicies of answer candidates (A, B, C, D) to numeric Int indicies (0, 1, 2, 3)
  def letterAnswerToNumericIndex(in:String):Int = {
    if (in.size != 1) {
      return -1
    }

    // Normal case
    if (in.matches("[A-D]")) {
      return in.charAt(0) - 65
    } else if (in.matches("[1-4]")) {
      return in.toInt - 1
    }

    // Error
    -1
  }


  // Safely retrieve a field from a line of text, using the fieldMap.  Returns None if the field was not included.
  def safeGetField(fieldName:String, fieldMap:scala.collection.mutable.Map[String, Int], fields:List[String]):Option[String] = {
    if ( fieldMap(fieldName) == -1 ) return None
    // return
    Some( fields(fieldMap(fieldName)).trim() )
  }


  // Parse a list of focus words from a string field.  Filters additional characters that may be included in the field for annotation purposes.
  def parseFocusWords(in:String):Array[String] = {
    // Step 1: Filter out any non alphanumeric characters (including commas, quotes, etc).
    var filtered = in.replaceAll("[^a-zA-Z0-9 -]", "")      // Filter Non-alpha numeric characters
    filtered = filtered.replaceAll("\\s\\s+", " ")            // Collapse multiple whitespaces to a single whitespace
    filtered = filtered.trim()                              // Trim any leading/trailing whitespace
    filtered = filtered.toLowerCase                         // Convert to lower case

    // Step 2: Split
    val delim = " "                                         // space delimiter
    val split = filtered.split(delim)

    // Return
    split
  }


  // Returns Array of exam questions, and any warning text.
  def load(filename:String, tsvMode:Boolean = false):(Array[ExamQuestion], String) = {
    val questions = new ArrayBuffer[ExamQuestion]()
    val warningText = new StringBuilder

    // Optionally, adapt the TSV reader format to parse a straight TSV format.
    var lines:List[List[String]] = null;
    if (tsvMode) {
      // TSV mode
      implicit object MyFormat extends DefaultCSVFormat {
        //override val delimiter = delim
        override val delimiter = '\t'
        override val escapeChar = 0.toChar
        override val quoteChar = 0.toChar
      }
      val reader = CSVReader.open(new File(filename))
      lines = reader.all()
    } else {
      // CSV mode
      val reader = CSVReader.open(new File(filename))
      lines = reader.all()
    }

    // Read and interpret header
    val header = lines.slice(0, 1)(0)     // All fields from first line
    val (headerMap, headerWarningText) = getColumnMapping(header)
    logger.debug(" Found " + headerMap.size + " recognized fields in question file. ")
    logger.debug(" Header Map: " + headerMap.toString)
    if (headerWarningText.length > 0) {
      logger.warn(" ExamQuestionParserDynamic warnings: " + headerWarningText)
      warningText.append( headerWarningText )
    }

    if (headerMap.size == 0) throw new RuntimeException(" ERROR: Could not find recognized header labels for question file (" + filename + ").  First line of question file must include column header with field names (QuestionID, Question, AnswerKey, etc...) .  ")


    // Strip header
    lines = lines.slice(1, lines.size)

    for (fields <- lines) {
      //logger.debug( fields.toList.toString() )
      //logger.debug(" fields.size = " + fields.size)

      // Debug: Allow for blank lines in the question file, to delimit groups of questions.
      breakable {
        // Read question text
        var question = safeGetField(LABEL_QUESTIONTEXT, headerMap, fields).getOrElse("")
        println ("Question Text: " + question)

        // If question text is blank, then skip this line
        if (question.length < 2) break()

        // Check for/remove quotes encasing the question text
        if ((question(0) == '\"') && (question(question.size - 1) == '\"')) {
          question = question.substring(1, question.size - 1)
        }


        // Read in fields
        val qID                       = safeGetField(LABEL_QUESTIONID, headerMap, fields).getOrElse("")
        val originalID                = safeGetField(LABEL_ORIGQUESTIONID, headerMap, fields).getOrElse("")
        val totalPoints               = safeGetField(LABEL_TOTALPOINTS, headerMap, fields).getOrElse("1").toInt
        val examName                  = safeGetField(LABEL_EXAMNAME, headerMap, fields).getOrElse("")
        val schoolGrade               = safeGetField(LABEL_GRADE, headerMap, fields).getOrElse("")
        val year                      = safeGetField(LABEL_YEAR, headerMap, fields).getOrElse("")
        val answerKey                 = letterAnswerToNumericIndex(safeGetField(LABEL_CORRECTANSWER, headerMap, fields).getOrElse(""))
        val topics                    = safeGetField(LABEL_TOPIC, headerMap, fields).getOrElse("").toUpperCase       // Topic cluster
        val goldExplanation           = safeGetField(LABEL_GOLDEXPLANATION, headerMap, fields).getOrElse("")

        // Read in extended fields
        val questionSimplified        = safeGetField(LABEL_QUESTIONSIMPLIFIED, headerMap, fields).getOrElse("")
        val questionSimplifiedConc    = safeGetField(LABEL_QUESTIONSIMPLIFIEDCONC, headerMap, fields).getOrElse("")
        val questionFocusPrimary      = parseFocusWords( safeGetField(LABEL_QUESTIONFOCUS_PRIMARY, headerMap, fields).getOrElse("") )
        val questionFocusSecondary    = parseFocusWords( safeGetField(LABEL_QUESTIONFOCUS_SECONDARY, headerMap, fields).getOrElse("") )
        val answerFocusA              = parseFocusWords( safeGetField(LABEL_ANSWERFOCUS_ANSA, headerMap, fields).getOrElse("") )
        val answerFocusB              = parseFocusWords( safeGetField(LABEL_ANSWERFOCUS_ANSB, headerMap, fields).getOrElse("") )
        val answerFocusC              = parseFocusWords( safeGetField(LABEL_ANSWERFOCUS_ANSC, headerMap, fields).getOrElse("") )
        val answerFocusD              = parseFocusWords( safeGetField(LABEL_ANSWERFOCUS_ANSD, headerMap, fields).getOrElse("") )

        val answerFocus = Array[Array[String]](answerFocusA, answerFocusB, answerFocusC, answerFocusD)

        val flags                     = safeGetField(LABEL_FLAGS, headerMap, fields).getOrElse("")
        val explanationAnnoatators    = safeGetField(LABEL_EXPLANATIONANNOTATORS, headerMap, fields).getOrElse("")

        // Convert flags into a Counter
        val flagsCounter = new Counter[String]
        for (flag <- flags.split(" ")) {
          flagsCounter.setCount(flag, 1.0)
        }


        // Read in fields that may require some additional processing
        var isMultipleChoice: Boolean = false
        if (safeGetField(LABEL_ISMCQUESTION, headerMap, fields).getOrElse("") == "1") isMultipleChoice = true

        var hasDiagram: Boolean = false
        if (safeGetField(LABEL_ISDIAGRAM, headerMap, fields).getOrElse("") == "1") hasDiagram = true


        // Inference type (may or may not be present)
        var questionType = safeGetField(LABEL_KNOWLEDGETYPE, headerMap, fields).getOrElse("")
        // Remove possible trailing "?" on question type
        if ((questionType.size > 1) && (questionType(questionType.size - 1) == '?')) {
          questionType = questionType.substring(0, questionType.size - 1)
        }

        // Extract multiple choice answer candidates from multiple choice questions
        if (isMultipleChoice) {
          val (questionText, mcAnswers) = textToMC(question)

          // Store Regents MC Question
          val rq = new ExamQuestion(qID, originalID, totalPoints, answerKey, isMultipleChoice,
            hasDiagram, examName, schoolGrade, year, questionType, topics, goldExplanation, questionText, mcAnswers,
            flagsCounter, explanationAnnoatators,
            questionSimplified, questionSimplifiedConc, questionFocusPrimary, questionFocusSecondary, answerFocus)
          questions.append(rq)

        } else {
          val rq = new ExamQuestion(qID, originalID, totalPoints, answerKey, isMultipleChoice,
            hasDiagram, examName, schoolGrade, year, questionType, topics, goldExplanation, question, Array.empty[String],
            flagsCounter, explanationAnnoatators,
            questionSimplified, questionSimplifiedConc, questionFocusPrimary, questionFocusSecondary, answerFocus)
          questions.append(rq)
        }
      }

    }

    // Return
    (questions.toArray, warningText.toString)
  }


  def toMCQuestion(in:Array[ExamQuestion], fullAnnotation:Boolean = true, noAnnotation:Boolean = false, annotateExplanations:Boolean = true):Array[MCQuestion] = {
    val out = new Array[MCQuestion](in.size)

    for (i <- 0 until in.size) {
      val question = in(i)
      val choices = new Array[MCAnswer](question.choices.size)

      // Annotate multiple choice answers
      for (j <- 0 until choices.size) {
        val aText = question.choices(j)
        //val answer = new MCAnswer(aText, mkPartialAnnotation(aText))
        var answer:MCAnswer = null
        if (fullAnnotation) {
          answer = new MCAnswer(aText, processor.annotate(aText), Array.empty[String])
        } else if (noAnnotation == false) {
          answer = new MCAnswer(aText, mkPartialAnnotation(aText), Array.empty[String])
        } else {
          answer = new MCAnswer(aText, null, Array.empty[String])
        }
        choices(j) = answer
      }
      // Annotate question
      //val qAnnotation = mkPartialAnnotation(question.text)
      var qAnnotation:Document = null
      var jAnnotation:Document = null
      if (fullAnnotation) {
        qAnnotation = processor.annotate( question.text )
        if (annotateExplanations) jAnnotation = processor.annotate( question.explanation )
      } else if (noAnnotation == false) {
        qAnnotation = mkPartialAnnotation( question.text )
        if (annotateExplanations) jAnnotation = mkPartialAnnotation( question.explanation )
      }

      // Split topic(s)
      val topics = new ArrayBuffer[String]
      for (topic <- question.topics.split(",")) {
        topics.append( topic.toUpperCase.trim )
      }

      var hasFocusWords:Boolean = false
      if (question.questionFocusPrimary.size > 0) hasFocusWords = true

      var qSimplified:Option[String] = None
      if (question.questionSimplified.length > 0) qSimplified = Some(question.questionSimplified)

      var qSimplifiedConc:Option[String] = None
      if (question.questionSimplifiedConc.length > 0) qSimplifiedConc = Some(question.questionSimplifiedConc)


      out(i) = new MCQuestion(question.text, qAnnotation, question.questionType, question.explanation, jAnnotation, question.schoolGrade, topics.toArray, question.qID,
        question.correctAnswer, choices, question.flags, question.explanationAnnotators,
        hasFocusWords, question.questionFocusPrimary, question.questionFocusSecondary,
        qSimplified, qSimplifiedConc)
    }

    out
  }

  def mkPartialAnnotation(text:String):Document = {
    val doc = processor.mkDocument(text)
    processor.tagPartsOfSpeech(doc)
    processor.lemmatize(doc)
    doc.clear()
    doc
  }



  def main(args:Array[String]) {
    val props = StringUtils.argsToProperties(args)

    /*
    val path:String = "/data/nlp/corpora/AriResources/AI2-Elementary-Feb2016/"
    val filenames = new ArrayBuffer[String]
    filenames.append(path + "Elementary-NDMC-Train.csv")
    */


    val path:String = "questions/AI2-elementary/"
    val filenames = new ArrayBuffer[String]
    filenames.append(path + "Elementary-NDMC-TrainDev-withlicensed-category.csv")


    parse( filenames.toArray )

  }

}