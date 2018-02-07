# README.md (worldtree-api)
***

### Background
The Worldtree project aims to produce methods of automated inference for question answering that are able to combine multiple pieces of information ("information aggregation") to answer questions.  A central focus of Worldtree is on explanation-centered inference -- producing not only correct answers, but also compelling human-readable justifications for why the answers to those questions are correct.  These explanations should be generated and used by the inference process to correctly answer a given question.  The central evaluation mechanism is through standardized science exam questions, which contain a variety of very challenging forms of inference (Clark AKBC 2013, Jansen et al. COLING 2016). 

### Repository 
This repository is for public releases of the Worldtree project.  Code and data will be moved from the research repository to this repository once papers are accepted so that the code and analyses are easily replicable and can be reused by others. 

We hope this code and data are useful to you.  Please feel free to get in touch if you have any questions, comments, or difficulties ( pajansen@email.arizona.edu ).

### Licenses
Please see `README_LICENSES.md`, included in this repository.


### More Information
The following papers describe more about the task of information aggregation, and explanation generation in the context of question answering.  These papers are available at http://www.cognitiveai.org/publications : 
  - **Fried et al. (TACL 2015) Higher-order Lexical Semantic Models for Non-factoid Answer Reranking:** We investigate a large number of multi-hop models for question answering that traverse word-level graphs built using a variety of lexical semantic methods, representations, and traversal methods including PageRank.  Using a corpus of 10,000 questions from Yahoo! Answers, we experimentally demonstrate that higher-order methods are broadly applicable to alignment and language models, across both word and syntactic representations. We show that an important criterion for success is controlling for the "semantic drift" that accumulates during graph traversal.  Without controlling for semantic drift, performance rarely increases beyond 2 hops. 
  
  - **Jansen et al. (COLING 2016) What’s in an Explanation? Characterizing Knowledge and Inference Requirements for Elementary Science Exams:** We perform a fine-grained analysis of the knowledge and inference requirements required to answer and provide compelling human-readable explanations for elementary science exams by generating a small corpus of free-text explanations, then annotating these for common knowledge and inference types.  We identify 3 coarse-grained and 21 fine-grained knowledge types that serve as the basis for future work in developing large corpora of structured explanations.  We also empirically validate our analysis by showing that of two solvers using different mechanisms (one a retrieval or look-up solver, the other an "inference" solver), the "inference" solver successfully answers more questions requiring complex inference. 
   
  - **Jansen et al. (CL 2017) Framing QA as Building and Ranking Answer Justifications:**  We show it is possible to learn to aggregate free-text sentences from study guides to build partial explanations that answer elementary science questions.  Because no explicit training data for the explaination construction task was available, we showed it was possible to learn this task latently.  Performance benefits are shown when aggregating 2 and up to 3 free text sentences, after which semantic drift likely prevents further increases. 
  
  - **Jansen (AKBC 2017) A Study of Automatically Acquiring Explanatory Inference Patterns from Corpora of Explanations: Lessons from Elementary Science Exams:** Constructing large explanations (more than 2 facts) by aggregating separate facts/sentences is very hard.  This is very limiting, as we have shown that even elementary science exam questions require aggregating an average of 4 to 6 facts (and sometimes many more) to answer and explain the details of that inference.  In this paper we explore whether new explanations can be constructed by reusing patterns (at various levels of abstraction) found in known explanations.  We empirically demonstrate that there are sufficient common explanatory patterns in the Worldtree corpus that it is possible in principle to reconstruct unseen explanation graphs by merging multiple explanatory patterns, then adapting and/or adding to their knowledge. This may ultimately provide a mechanism to allow inference algorithms to surpass the two-fact “aggregation horizon” in practice by using common explanatory patterns as constraints to limit the search space during information aggregation.
  
  - **Jansen et al. (LREC 2018): WorldTree: A Corpus of Explanation Graphs for Elementary Science Questions supporting Multi-hop Inference:** We construct and analyze a large corpus of 1,680 explanations for elementary science questions, represented as lexically connected "explanation graphs", to support efforts in supervised multi-hop inference.  Each sentence in an explanation is represented as a row in a semi-structured table, and each row in an explanation must have shared words (lexical overlap) with the question, answer, and/or another sentence in the explanation.  The average explanation is 6 sentences/table rows in length.  The semi-structured representation of explanations facilities a variety of automated analyses, and we show a number of predictable properties and relationships of this explanation corpus (knowledge frequency, explanation overlap and cluster size, and knowledge growth and reuse) that we hypothesize may apply to other corpora of semi-structured explanations when they are available.  To the best of our knowledge, this corpus of explanations is unique both in it's scale (nearly everly publicly available elementary science question is included) and commitment to providing semi-structured relations from the level of complete explanations to individual sentences within explanations).



***
# Release-specific notes
### LREC 2018 (Worldtree Corpus V1.0 Initial Release, Analysis of Explanation Corpus)
This repository includes the analyses for the LREC 2018 paper, "Worldtree: A Corpus of Explanation Graphs for Elementary Science Questions supporting Multi-hop Inference" (Jansen, Wainwright, Marmorstein, and Morrison). The paper is available at  http://www.cognitiveai.org/publications .

The analyses include: 
  - **explanationexperiments.SummaryStatistics:** Basic summary statistics (e.g. average explanation length)
  - **explanationexperiments.MostCommonTableRows:** (1) The proportion of explanations that contain knowledge from a given table (Table 3), as well as (2) an analysis sorting the most commonly reused table rows across explanations by frequency, illustrating a sort of Zipf's law for explanation corpora (only discussed briefly in the LREC 2018 paper). 
  - **explanationexperiments.CharacterizeConnectivity:** The monte-carlo simulation to characterize the proportion of questions whose explanations overlap by 1 or more, 2 or more, 3 or more, etc, explanation sentences (Figure 5). The simulation also measures the average cluster size of questions with a given level of connectivity (footnote 6).
  - **explanationexperiments.SummaryKnowledgeGrowth:** * The monte-carlo analysis to determine the number of unique table rows required to explainably answer a given number of questions. This simulation generates the data for Figure 6 in the LREC2018 paper.
 
The repository also includes:
  - **explanationexperiments.GenerateQuestionClusterGraph:** The conversion tool that exports connectivity graphs in DOT format.  This generates the cool explanation connectivity/overlap graphs, such as Figure 4 from the paper.  Gephi ( https://gephi.org/ ) is used to import, style, and render these graphs. 
  - An easy-to-understand example, **examples.LoadQuestionsTablestore**, illustrating the use of the API, parsers, and storage classes for loading and making use of both the questions and tablestore annotation.

##### Corpora
This repository includes a partial release of the Worldtree V1.0 corpus.  The full corpus including additional formats, data, and visualizations is available at http://www.cognitiveai.org/explanationbank .


##### Running these experiments

The experiments should run as-is, and require only reference to an external property file ( `props/worldtreecorpus.lrec2018.properties` ) that includes paths to the corpus, and modifiable parameters for each analysis/tool.  Each of the experiments may require more than the default amount of memory on your machine, particularly the monte-carlo analyses, so you may need to increase this in your IDE or on the command line (e.g. -Xmx8g ).

An example of configuring the command line paramters and memory to run one of the analyses in IntelliJ is as follows:
![alt text](https://github.com/clulab/worldtree-api/raw/master/README/intellij_screenshot_lrec2018.jpg "alt text")


***
### LREC 2018 (Explanation Annotation Tool)

**TODO:** This tool will be released once it has been packaged, and installation instructions have been written. 



