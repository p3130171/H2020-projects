Cordis Project Graph
====================
Abstract
--------
The current project is a data searching tool for [Cordis EU](cordis.europa.eu) Research Projects built on top of Graph Database concepts.

How it works
------------
![howitworks](static/howitworks.png "How it Works")  

- First, the local Cordis dataset (1) is being analysed and processed, fetching the info's for the Projects and their Contributors (2).  
- A Lucene core is spawned inside the host program which then indexes all the Projects with their info's (3).  
- Queries are performed for each one of them in order to determine the most relatively similar Projects, building a relativity list (4).  
- The Graph Database is populated with the Project and Contributor information by creating the associated nodes and then the Project nodes are linked with their Contributor nodes and related Project nodes respectively (5).  
- A Web Server instance spawns, that serves the REST API entrypoints for data requests about the database nodes/relations and project search functionality but also an internal web client to that utilizes this, as a sample for the end client (6).

Features
--------
 * All modules are implemented under same codebase in Python language and inside a single script for great portability
 * Uses [Py2Neo](http://py2neo.org) client toolkit for interaction with Neo4j Database
 * Uses [PyLucene](http://lucene.apache.org/pylucene/) extension to run an instance of LuceneVM inside our program, avoiding any additional server setups unlike Solr/ElasticSearch
 * Implements a REST API for retrieving requests and passing data to clients interactively
 * Implements a mini web client using [Flask](http://flask.pocoo.org/) that utilizes the exposed REST API and displays results as an interactive graph in the browser
 * Internal web client can perform search queries about Projects using the LuceneVM instance in the host program and return information about them, their contributors and their associations

Technologies
------------
 * [Neo4j Graph Database](https://neo4j.com/)
 * [Apache Lucene Search Engine](https://lucene.apache.org/core/)
 * [Alchemy JS Graph Visualization](http://graphalchemist.github.io)
 * [Flask Web Framework](http://flask.pocoo.org/)

Contributors
------------
 * Agorgianitis Loukas <<agorglouk@gmail.com>> (Original Author/Programmer)
 * Theodore Kalamboukis <<tzk@aueb.gr>> (Original Lead Research Consultant)
 * Petrocheilos Alkiviadis <<alkis.petrochilos@gmail.com>> (Programmer)
