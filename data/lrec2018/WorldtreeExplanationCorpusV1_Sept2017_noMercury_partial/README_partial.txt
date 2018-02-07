** Note, this is only a partial distribution included with worldtree-api intended to **
** demonstrate the functionality of the accompanying code.                           **
** Please visit http://www.cognitiveai.org/explanationbank for the full distribution **


Worldtree: A corpus of Explanation Graphs for Elementary Science Questions
           supporting Multi-hop Inference
Version 1.0 (September 2017)
http://www.cognitiveai.org/explanations
===================================

This README describes the usage of the WorldTree corpus of explanations to
standardized elementary science questions (3rd through 5th grade), which also
form lexically-connected explanation graphs.  The corpus also contains a
semistructured Tablestore knowledge resource of elementary science and world
knowledge. 


LICENSE
========
This work combines several datasets, and is distributed under mixed licenses. 
The questions in this corpus are drawn from the AI2 Science Questions V2 corpus,
as well as the separately licensed AI2 Science Questions Mercury dataset
containing science questions provided under license by a research partner 
affiliated with AI2.  A number of the tables in the Tablestore are drawn from 
the AI2 Aristo Tablestore.

For more information, please visit http://www.allenai.org/data.html .


1) AI2 Mercury Dataset
-----------------------------------------
* Do not distribute *
* Non-commercial use only *
The terms of this data set's license agreement stipulate that this data should not 
be distributed except by the Allen Institute for Artificial Intelligence (AI2), and 
only with adherence to the terms contained in the End User License Agreement
(included separately in this archive).
 
All parties interested in acquiring this data must download it from AI2 directly 
and follow the terms of the the EULA, which specifies the data is to be used for 
non-commercial, research purposes only.

Please contact ai2-data@allenai.org with any questions regarding AI2’s data sets.

2) Tablestore and Explanation Annotation
-----------------------------------------
The Tablestore and explanations themselves (separate from the questions) are 
distributed under a CC-BY-SA license.  The Tablestore includes a number of 
tables drawn from the AI2 Aristo Tablestore, which is also distributed under
CC-BY-SA. 

The Creative Commons Attribution-ShareAlike 4.0 International License (http://creativecommons.org/licenses/by-sa/4.0/legalcode)

This means you are free to:
1) Share — copy and redistribute the material in any medium or format
2) Adapt — remix, transform, and build upon the material
for any purpose, even commercially.
The licensor cannot revoke these freedoms as long as you follow the license terms.

Under the following terms:
1) Attribution — You must give appropriate credit, provide a link to the license, and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.
2) ShareAlike — If you remix, transform, or build upon the material, you must distribute your contributions under the same license as the original.
3) No additional restrictions — You may not apply legal terms or technological measures that legally restrict others from doing anything the license permits.


ATTRIBUTION
============
If you make use of this resource in your work, please cite the following paper:

Jansen, Wainwright, Marmorstein, and Morrison (2018). Worldtree: A corpus of
Explanation Graphs for Elementary Science Questions supporting Multi-hop 
Inference.  Proceedings of the Language Resource and Evaluation Conference 
(LREC 2018). 



USAGE
======
The corpus is distributed in three separate formats: (1) a set of tab-delimited files
describing the questions, explanations, and tablestore, (2) a set of Excel files with
the original version of the corpus prior to export, and (3) a plain-text version of
the questions and explanations for easy review. 

* (1) Tab-delimited Questions, Explanations, and Tablestore. 

The primary method of use is through this format, a series of tab-delimited files that
describe the questions, explanations, and tables from which the explanation sentences
are drawn. 

"tsv/questionsAndExplanations.tsv" is in the AI2 questions corpus format, and includes
an additional "explanation" field.  This field contains a series of unique identifiers
(UIDs) representing specific table rows, as well as the 'explanatory role' that each 
table row takes on in a given explanation.  Within the explanation field, the UIDs:Role
tuples are pipe-delimited ("|"), and separate UID:Role tuples are space-delimited. 

For example, for the first question:

Q: Which of the following is an example of a form of energy? 
(A) the air in a sealed jar (B) the wire in a metal hanger (C) the water in a small puddle (D) the sound in a loud classroom

The "explanation" field takes the form of four sentences, themselves represented as four 
UIDs referencing specific table rows:

1980-256d-b685-846c|CENTRAL 9f4e-2412-59cc-3383|CENTRAL 7c96-f704-e51e-1678|LEXGLUE 1b3f-b617-d7ef-1815|LEXGLUE

By looking up these rows in the 63 tables within the tablestore, the plain text 
explanation can be reconstructed: 

Question: Which of the following is an example of a form of energy?	[0]: the air in a sealed jar	[1]: the wire in a metal hanger	[2]: the water in a small puddle	[3]: the sound in a loud classroom	
Correct Answer: 3
Explanation: 
sound is a kind of energy (UID: 1980-256d-b685-846c) (ROLE: CENTRAL)
loud means high in (sound energy ; sound intensity) (UID: 9f4e-2412-59cc-3383) (ROLE: CENTRAL)
form means kind (UID: 7c96-f704-e51e-1678) (ROLE: LEXGLUE)
a kind of something is an example of that something (UID: 1b3f-b617-d7ef-1815) (ROLE: LEXGLUE)


* (2) Excel files

As above, but in a Microsoft Excel format.  Some additional comments on rows are included in this 
version of the corpus. 


* (3) Plain-text

A plain-text version of the corpus for easy review is also included, in the file
"explanations_plaintext.txt" 


TABLESTORE FORMAT
==================

The first line of each table represents the table header, where each following line 
represents the rows of a given table.  A given column in a table header may be 
preceded by a prefix that marks the column as either filler (e.g. "[FILL]") that
allows the row to be read off as a natural language sentence, or take the form of 
a "[SKIP]" column with meta-data: 

[SKIP] COMMENTS: Annotator comments on this row (if any)
[SKIP] DEP: The tablestore is a living document, and rows are occasionally 
  refactored or moved if they can be better represented in another location. 
  Having text populated in the "DEPrication" column represents that a given
  table row should not actively be used in explanation construction. 
[SKIP] UID: The unique identifier string for a given table row. 



