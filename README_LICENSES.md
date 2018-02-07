# README_LICENSES.md (worldtree-api)
***

# Data
***
##### 1. Worldtree Corpus V1.0 (Sept 2017 Snapshot)
This repository includes a partial version of the Worldtree Corpus V1.0 (Sept 2017) snapshot, which includes questions, explanations, and tablestore annotation.  The full version is available at http://cognitiveai.org/explanationbank 
  - AI2 Science Questions: Included in the WorldTree corpus. The original version and documentation are available from AI2 ( http://allenai.org/data.html )
  - AI2 Science Questions Mercury: A separately licensed set of questions with a non-commercial, non-distribution license.  These questions are not included, but are available from AI2. 
  - Aristo Tablestore: A number of the 62 tables in the WorldTree Tablestore are reused from the Aristo Tablestore ( http://allenai.org/data.html ), which is distributed under a CC-BY-SA license. 

##### 2. Lemmatization List
There are not currently any online lemmatizers available for the node.js annotation webtool that we're aware of, so the webtool makes use of a look-up lemmatizer.  Some of the Worldtree code also makes use of this lemmatizer for
consistency (LookupLemmatizer.scala).  
  - The list of English lemmatizations is from Michal Boleslav Měchura ( http://www.lexiconista.com/datasets/lemmatization/http://www.lexiconista.com/datasets/lemmatization/ ).  We have added a small number of domain-specific lemmatizations to this list.
  
  
# Code
***
