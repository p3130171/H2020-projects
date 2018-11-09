#!/usr/bin/env python3
import sys
import re
import string
import csv
import json
import argparse
import lucene
import re

from py2neo import Graph
from flask import Flask, Response, request
from contextlib import contextmanager
from org.apache.lucene.analysis.standard import StandardAnalyzer
from org.apache.lucene.document import Document
from org.apache.lucene.document import Field
from org.apache.lucene.document import StringField
from org.apache.lucene.document import TextField
from org.apache.lucene.index import DirectoryReader
from org.apache.lucene.index import IndexWriter
from org.apache.lucene.index import IndexWriterConfig
from org.apache.lucene.queryparser.classic import QueryParser
from org.apache.lucene.search import IndexSearcher
from org.apache.lucene.store import RAMDirectory

#----------------------------------------------------------------------
# Datasets
#----------------------------------------------------------------------
PROJECT_DATAFILE='data/cordis-h2020projects-MODIFIED.csv'

#----------------------------------------------------------------------
# Global list variables
#----------------------------------------------------------------------
titleHitsList = {}
objectiveHitsList = {}
mergedList = {}

#----------------------------------------------------------------------
# Related projects using Lucene
#----------------------------------------------------------------------
@contextmanager
def closing(thing):
    """ Simple wrapper to make Lucene's classes appear more pythonic. """
    try:
        yield thing
    finally:
        thing.close()

def make_index(analyzer, dataset):
    """ Create an inverted index to power the search. """

    def add_doc(w, rcn, objective, title, call):
        """ Utility to add "documents" to the index. """
        doc = Document()
        # Use a string field for rcn because we don't want it tokenized
        doc.add(StringField("rcn", rcn, Field.Store.YES))
        doc.add(TextField("objective", objective, Field.Store.YES))
        doc.add(TextField("title", title, Field.Store.YES))
        doc.add(TextField("call", call, Field.Store.YES))
        w.addDocument(doc)

    # Create the index
    index = RAMDirectory()

    config = IndexWriterConfig(analyzer)
    with closing(IndexWriter(index, config)) as w:
        for row in dataset:
            rcn = row["rcn"]
            obj = row["objective"]
            title = row["title"]
            call = row["call"]
            call = re.sub("-|_", "", call) # Remove special characters from the call.
            add_doc(w, rcn, obj, title, call)
    return index

def query(querystr, index, analyzer, hits_per_page):
    """ Search for the `querystr` in the index. """
    # Escape special characters
    querystr = QueryParser.escape(querystr)
    # BUG: Lucene throws exception if last character of query is an escaped character.
    # It is possibly trying to strip out newlines or something, so we add a whitespace
    # character to avoid it.
    querystr = querystr + '\n'

    # The "objective" arg specifies the default field to use
    # when no field is explicitly specified in the query.
    q = QueryParser("objective", analyzer).parse(querystr)
    
    # Search
    related_projects = []
    with closing(DirectoryReader.open(index)) as reader:
        searcher = IndexSearcher(reader)
        docs = searcher.search(q, hits_per_page)
        hits = docs.scoreDocs
        for i, hit in enumerate(hits):
            docId = hit.doc
            d = searcher.doc(docId)
            related_projects.append(d.get("rcn"))
         
    return related_projects

def searchField(analyzer, hits_per_page, field, query):
    
    searchHitsList = {}
    query = QueryParser.escape(query)
    query = query + '\n'
    q = QueryParser(field, analyzer).parse(query) # The queries contain empty lines and other special characters. The empty lines are skipped with "escape".
    
    
    with closing(DirectoryReader.open(index)) as reader:
        searcher = IndexSearcher(reader)
        docs = searcher.search(q, hits_per_page)
        hits = docs.scoreDocs
        for i, hit in enumerate(hits):
            docId = hit.doc
            d = searcher.doc(docId)
            searchHitsList[d.get("rcn")] = hits[i].score
            
        return searchHitsList
    
def related_projects(dataset, analyzer, index, titleWeight, objectiveWeight, callBonus):
    global titleHitsList, objectiveHitsList, mergedList
    print('[+] Performing queries...')
    related_projects = {}
    hits_per_page = 10
    
    for i, row in enumerate(dataset):
        print('\r[+] Processing {} of {}'.format(i, len(dataset) - 1), end='')
        
        #Empty the lists for every project in the dataset.
        titleHitsList.clear()
        objectiveHitsList.clear()
        mergedList.clear()
        call = row["call"] # Save the category of the main node.
        call = re.sub("-|_", "", call) # Remove the special characters from the call.
        
        # Search for the title.
        titleQuery = row["title"]
        titleHitsList = searchField(analyzer, hits_per_page, "title", titleQuery)
        
        # Search for the objective.
        objectiveQuery = row["objective"]
        objectiveHitsList = searchField(analyzer, hits_per_page, "objective", objectiveQuery)
        
        # Put all in the merged list.
        # The title hits list score will overwrite the objective hits list score.
        mergedList = objectiveHitsList.copy()
        mergedList.update(titleHitsList)
        
        with closing(DirectoryReader.open(index)) as reader:
            searcher = IndexSearcher(reader)
            titleScore = 0
            objectiveScore = 0
            
            for key, score in mergedList.items(): # For every related project.
                
                callHit = 0 # If 0 then there is no hit in "call", otherwise it is a hit.
                # If the key exists, then get the title score, else return 0 as its score.
                titleScore = titleHitsList.get(key, 0)
                # If the key exists, then get the objective score, else return 0 as its score.
                objectiveScore = objectiveHitsList.get(key, 0)
                d= QueryParser("rcn", analyzer).parse(key)
                doc = searcher.search(d, 1)
                test = doc.scoreDocs
                docId = test[0].doc
                d = searcher.doc(docId)
                
                if (d.get("call").lower() == call.lower()): # If there is a call hit.
                    callHit = 1
                
                # Calculate the score
                totalScore = titleScore * titleWeight + objectiveScore * objectiveWeight + callHit * callBonus
                mergedList[key] = totalScore # Update the score in the final score list.
                
            sortedMergedList = sorted(mergedList.items(), key=lambda kv: kv[1], reverse = True) # Sort the list.
            
        max_results = 10 + 1
        i = 0
        related = []
        while i < len(sortedMergedList): # Get the best max_results
            
            if(i == max_results):
                break
            
            result = re.search('\'(.*)\'', str(sortedMergedList[i])) # Parse the string and get only the rcn number.
            related.append(result.group(1))
            i += 1
            
        # Filter out self
        related = [x for x in related if x != row["rcn"]]
        related_projects[row["rcn"]] = related
    
    print('\n[+] Done.')
    return related_projects
    
#----------------------------------------------------------------------
# Cypher queries
#----------------------------------------------------------------------
# Connect to Neo4j instance using py2neo
graphdb = Graph('http://neo4j:qwer1234@localhost:7474/db/data')

COUNT_NODES_QUERY = '''
    MATCH (n)
    RETURN count(*)
'''

CLEAR_DATABASE_QUERY = '''
    MATCH (n)
    OPTIONAL MATCH (n)-[r]-()
    DELETE n, r
'''

CREATE_PROJECT_QUERY = '''
    CREATE (p:Project {props})
    FOREACH (n IN {names} |
        MERGE (c:Contributor {name: n})
        CREATE (c)-[:CONTRIBUTES]->(p)
    )
'''

#----------------------------------------------------------------------
# GraphDB Actions
#----------------------------------------------------------------------
# Returns the number of words in database
def count_nodes():
    r = graphdb.data(COUNT_NODES_QUERY)
    return r[0]['count(*)']

# Completely clears nodes and all relations from database
def clear_db():
    tx = graphdb.begin()
    tx.append(CLEAR_DATABASE_QUERY)
    tx.commit()

# Inserts a Project and Contributor nodes into the database with their CONTRIBUTES relation
def create_nodes(dataset):
    for i, row in enumerate(dataset):
        log = '\r[+] Processing project {} of {}'.format(i, len(dataset) - 1)
        print(log, end='')
        prj_info = dict(row)
        participants = prj_info.pop('participants').split(';')
        coordinator = prj_info.pop('coordinator').split(';')
        if("" in participants): # Remove empty participants
            participants.remove("")
            
		# Insert query
        tx = graphdb.begin()
        participants.append(coordinator) # Add the coordinators to the contributors
        tx.append(CREATE_PROJECT_QUERY, {'props': prj_info, 'names': participants})
        tx.commit()
    print('\n[+] Project data imported.')

# Inserts related project relations
def relate_projects(prj_id, related):
    tx = graphdb.begin()
    tx.append('''
    MATCH (p:Project), (o:Project)
    WHERE p.rcn = {prj_id} AND o.rcn in {related_ids}
    MERGE (p)-[:RELATED_TO]->(o)
    ''', {'prj_id': prj_id, 'related_ids': related})
    tx.commit()

def query_project(pid):
    r = graphdb.data('''MATCH (p:Project) WHERE id(p)={pid} RETURN p''', {'pid': int(pid)})
    if r:
        return r[0]['p']
    else:
        return None

def query_related_projects(pid):
    r = graphdb.data('''MATCH (p:Project)-[:RELATED_TO]->(r:Project)
                        WHERE id(p)={pid} RETURN id(r)''', {'pid': int(pid)})
    return [k['id(r)'] for k in r]

def query_pid_from_rcn(rcn):
    r = graphdb.data('''MATCH (p:Project)
                        WHERE p.rcn={rcn} RETURN id(p)''', {'rcn': rcn})
    return r[0]['id(p)']

def query_contributors(pid):
    r = graphdb.data('''MATCH (p:Project)<-[:CONTRIBUTES]-(c:Contributor)
                        WHERE id(p)={pid}
                        RETURN id(c), c''', {'pid': int(pid)})
    return [(p['id(c)'], p['c']) for p in r]

#----------------------------------------------------------------------
# Web server
#----------------------------------------------------------------------
app = Flask(__name__, static_url_path='/static')

@app.route('/')
def hello():
    return app.send_static_file('index.html')

@app.route('/search')
def search():
    global index, analyzer
    # Attach lucene module to webserver's thread
    vm_env = lucene.getVMEnv()
    vm_env.attachCurrentThread()
    # Perform query
    q = request.args.get('query')
    max_results = 10
    related_rcns = query(q, index, analyzer, max_results)
    # Fetch projects with given rcns
    nodes = []
    relations = []
    for rcn in related_rcns:
        pid = query_pid_from_rcn(rcn)
        prj = query_project(pid)
        n = {
            'id'     : pid,
            'type'   : 'project',
            'data'   : prj,
            'caption': prj['acronym']
        }
        nodes.append(n)
    return Response(json.dumps({"nodes": nodes, "edges": relations}), mimetype="application/json")

@app.route('/related/<pid>')
def fetch_related(pid):
    nodes = []
    added_node_ids = []
    relations = []
    # Add Main project node
    mprj = query_project(pid)
    n = {
        'id'     : pid,
        'type'   : 'project',
        'data'   : mprj,
        'caption': mprj['acronym']
    }
    nodes.append(n)
    added_node_ids.append(pid)
    # Add its contributors
    mcontributors = query_contributors(pid)
    for cid, contr in mcontributors:
        if cid not in added_node_ids:
            n = {
                'id'     : cid,
                'type'   : 'contributor',
                'data'   : contr,
                'caption': contr['name']
            }
            nodes.append(n)
            added_node_ids.append(cid)
        relations.append({
            "source": cid,
            "target": pid,
            "caption": "CONTRIBUTES"
        })

    # Add related projects
    related_prj_ids = query_related_projects(pid)
    for rpid in related_prj_ids:
        rprj = query_project(rpid)
        n = {
            'id'     : rpid,
            'type'   : 'project',
            'data'   : rprj,
            'caption': rprj['acronym']
        }
        nodes.append(n)
        relations.append({
            "source": pid,
            "target": rpid,
            "caption": "RELATED_TO"
        })
        # Add its contributors
        contributors = query_contributors(rpid)
        for cid, contr in contributors:
            if cid not in added_node_ids:
                n = {
                    'id'     : cid,
                    'type'   : 'contributor',
                    'data'   : contr,
                    'caption': contr['name']
                }
                nodes.append(n)
                added_node_ids.append(cid)
            relations.append({
                "source": cid,
                "target": rpid,
                "caption": "CONTRIBUTES"
            })
    return Response(json.dumps({"nodes": nodes, "edges": relations}), mimetype="application/json")

#----------------------------------------------------------------------
# Entrypoint
#----------------------------------------------------------------------
def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("-c", action="store_true", help="Clear the GraphDB")
    return parser.parse_args()

def main():
    global index, analyzer, titleHitsList, objectiveHitsList, mergedList

    # Parse command line arguments
    args = parse_args()

    # Clear database if specified
    if args.c:
        print("[+] Resetting database ...")
        clear_db()
        return

    # Load dataset
    print('[+] Loading dataset ...')
    dataset = csv.DictReader(open(PROJECT_DATAFILE, encoding='utf-8-sig', newline=''), delimiter=';')
    dataset = list(dataset)

    # Initialize lucene
    print('[+] Initializing Lucene VM...')
    lucene.initVM()

    # Specify the analyzer for tokenizing text.
    # The same analyzer should be used for indexing and searching
    analyzer = StandardAnalyzer()

    # Create the index to search
    print('[+] Creating index...')
    index = make_index(analyzer, dataset)

    # Populate database if empty
    num_nodes = count_nodes()
    print('[+] Found %d nodes in database!' % (num_nodes))
    if num_nodes == 0:
        print('[+] Inserting nodes ...')
        create_nodes(dataset)
        print('[+] Building relation data ...')
        relations = related_projects(dataset, analyzer, index, 1.5, 0.5, 100)
        for i, (relation, related) in enumerate(relations.items()):
            log = '\r[+] Processing relation {} of {}'.format(i, len(relations) - 1)
            print(log, end='')
            relate_projects(relation, related)
        print('\n[+] Relation data imported.')

    # Serve
    print('[+] Starting web server ...')
    app.run(host='0.0.0.0', port=8687, debug=True)

if __name__ == '__main__':
    main()
