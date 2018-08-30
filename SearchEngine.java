package search_engine_package;
import java.io.IOException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import org.xml.sax.InputSource;
import org.apache.lucene.analysis.Analyzer;
import java.util.Map.Entry;
import javax.swing.*;


public class SearchEngine {

	private static Map mergedList = new HashMap(); // Create a new list in order to merge the previous two.
	private static Map titleHitsList = new HashMap(); // List containing the title search hits.
	private static Map objectiveHitsList = new HashMap(); // List containing the objective search hits.
	private static Directory index;
	private static StandardAnalyzer analyzer;
	private static IndexWriter w;
	private static int hitsPerPage = 20; // Number of hits.
	private static IndexSearcher searcher;
	private static IndexReader reader;
	private static String workingDirectory;
	private static int numberOfResultsToShow = 20; // How many results to be showed in the results export file.
	private static int counter; // Used only to export the first "hitsPerPage" results in the text file.

	// Variables that will be accessed by the GUI class!
	private static String queriesFilePath;
	private static float titleWeight = (float) 1.5;
	private static float objectiveWeight = (float) 0.5;
	private static float callWeight = (float) 100.0;
	private static boolean readyToSearch = false; // If the indexing is done set true.

	public static void main(String[] args) throws IOException, ParseException {

		GUI window = new GUI(); // Create the GUI of the application.
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);

		workingDirectory = System.getProperty("user.dir"); // Get current working directory.
		String path = workingDirectory+"\\Parsed files\\";
		System.out.println("Working directory: " + workingDirectory);
		queriesFilePath = workingDirectory+"\\src\\search_engine_package\\Queries.xml";

		File directory = new File(path); // The directory containing all the xml files.
		File[] fileList = directory.listFiles(); // A list with all the xml files.
		System.out.println("Number of files in folder: "+fileList.length);

		// Specify the analyzer for tokenizing text.
		analyzer = new StandardAnalyzer();
		index = new RAMDirectory(); // Create the index in RAM.
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		w = new IndexWriter(index, config);

		/* Set the maximum limit of clauses in a BooleanQuery.
		"A Query that matches documents matching boolean combinations of other queries."
		This kind of query is not explicitly used here, but I double its limit just in case.
		If there is a clause limit problem, there will be an exception thrown.*/
		BooleanQuery.setMaxClauseCount(2048); // Double the limit (1024 by default).

		for (File file : fileList) { // For every file in the directory.

			if (file.getName().endsWith(".xml")) { // For all the xml files that exist in the directory.

				try { // Open the file in order to parse it and store the proper tags in NodeLists.

					String filePath = path+file.getName(); // The path of the current file.

					InputStream inputStream = new FileInputStream(filePath);
					InputStreamReader inputReader = new InputStreamReader(inputStream, "UTF-8");
					InputSource inputSource = new InputSource(inputReader);
					inputSource.setEncoding("UTF-8");
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					org.w3c.dom.Document test = dBuilder.parse(inputSource);

					String title = test.getElementsByTagName("title").item(0).getTextContent();
					// Parse the title from the xml file.

					String objective = test.getElementsByTagName("objective").item(0).getTextContent();
					// Parse the abstract from the xml file.

					String identifier = test.getElementsByTagName("identifier").item(0).getTextContent();
					// Parse the identifier field from the xml file.

					addDoc(w, title, objective, identifier); // Add the project in the lucene indexer.
					NodeList docID = test.getElementsByTagName("rcn"); // Extract the project id from the xml file.

				} catch (Exception e){

					System.out.println("Problem at file: "+file);
					e.printStackTrace();
				}

			}

		}

		readyToSearch = true; // Used to inform the GUI if the index is ready for search operation. The value of
		// this variable can be only viewed by the getReadyToSearch() method.
		w.close();

	}

	private static void addDoc(IndexWriter w, String title, String objective, String identifier) throws IOException {
		// Method to add a document in the indexer.

		Document doc = new Document();
		doc.add(new TextField("title", title, Field.Store.YES));
		doc.add(new TextField("objective", objective, Field.Store.YES));
		doc.add(new TextField("identifier", identifier, Field.Store.YES));
		w.addDocument(doc);
	}

	private static Map searchField(Analyzer analyzer, IndexSearcher searcher, int hitsPerPage, String field, String query) {
		// Method for searching a specific field for a query.

		Map searchHitsList = new HashMap(); // Create a list that contains the document id and its score.

		try {

			QueryParser searchParser = new QueryParser(field, analyzer); // Parser for the field of the document.
			Query searchQuery = searchParser.parse(QueryParser.escape(query)); // The query containing the field search.
			TopDocs searchDocs = searcher.search(searchQuery, hitsPerPage); // Search and store the hits.
			ScoreDoc[] searchHits = searchDocs.scoreDocs;

			for (ScoreDoc item : searchHits) { // For every item.

				searchHitsList.put((Integer)item.doc, (Float)item.score); // Populate the list with the doc id and the score.

			}

			System.out.println("\n"+field+" list: " + searchHitsList);
			System.out.println("-----------------------------");

		} catch (Exception e) {

			e.printStackTrace();

		}

		return searchHitsList; // Return the list with the results.

	}

	private static void calculateScore(int projectId, float titleWeight, float objectiveWeight, float idWeight, int identifier, IndexSearcher searcher) {
		//Method for calculating the score of a document.

		try {

			Document d = searcher.doc(projectId); // Get the document by its id.
			System.out.println("TITLE: " + d.get("title"));

		} catch(Exception e){e.printStackTrace();}

		float titleSimilarity = (float)0.0; // The title score.
		float objectiveSimilarity = (float)0.0; // The objective score.
		float score = (float)0.0; // The final score.

		if (titleHitsList.containsKey(projectId) && objectiveHitsList.containsKey(projectId)) {

			// Normal formula for scoring.
			titleSimilarity = (Float) titleHitsList.get(projectId);
			objectiveSimilarity = (Float) objectiveHitsList.get(projectId);

			score = titleWeight * titleSimilarity + objectiveWeight * objectiveSimilarity + idWeight * identifier;

		} else if (titleHitsList.containsKey(projectId) && !objectiveHitsList.containsKey(projectId)) {

			// Formula without objective similarity.
			titleSimilarity = (Float) titleHitsList.get(projectId);

			score = titleWeight * titleSimilarity + idWeight * identifier;

		} else if (objectiveHitsList.containsKey(projectId) && !titleHitsList.containsKey(projectId)) {

			// Formula without title similarity.
			objectiveSimilarity = (Float) objectiveHitsList.get(projectId);

			score = objectiveWeight * objectiveSimilarity + idWeight * identifier;

		} else { // Problem...

			System.out.println("Something went wrong in calculating the score of a document...");

		}
		System.out.println("SCORE: "+score+"\n");
		mergedList.replace(projectId, score); // Update the score of the document in the merged list.

	}

	public static void setTitleWeight(float titleWeight){ // Set the title weight (used by GUI class).

		SearchEngine.titleWeight = titleWeight;

	}

	public static void setObjectiveWeight(float objectiveWeight){ // Set the objective weight (used by GUI class).

		SearchEngine.objectiveWeight = objectiveWeight;
	}

	public static void setCallWeight(float callWeight){ // Set the objective weight (used by GUI class).

		SearchEngine.callWeight = callWeight;
	}

	public static void setQueriesFilePath(String queriesFilePath){ // Set the queries file path (used by GUI class).

		SearchEngine.queriesFilePath = queriesFilePath;
	}

	public static boolean getReadyToSearch() { // Find out if the application is ready to search.

		return readyToSearch;
	}

	public static void search(){ // Main searching method.

		System.out.println("SEARCH INITIATED!\n");
		System.out.println("titleWeight "+titleWeight);
		System.out.println("objectiveWeight "+objectiveWeight);
		System.out.println("callWeight "+callWeight);
		System.out.println("queries path "+queriesFilePath+"\n");

		try {

			reader = DirectoryReader.open(index);
			searcher = new IndexSearcher(reader);

		} catch(Exception k) {

			k.printStackTrace();

		}

		try {

			InputStream inStream = new FileInputStream(queriesFilePath);
			InputStreamReader inReader = new InputStreamReader(inStream, "UTF-8");
			InputSource inSource = new InputSource(inReader);
			inSource.setEncoding("UTF-8");
			DocumentBuilderFactory dbFactory2 = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder2 = dbFactory2.newDocumentBuilder();
			org.w3c.dom.Document queries = dBuilder2.parse(inSource);
			NodeList queryList = queries.getElementsByTagName("query");
			System.out.println("LENGTH "+queryList.getLength());

			String title; // Variable to store the title from the Queries.xml file.
			String objective; // Variable to store the objective from the Queries.xml file.
			String identifier; // Variable to store the identifier from the Queries.xml file.
			int identifierHit; // If the identifier is the searched one, then the value is 1. Otherwise it is 0.

			try (Writer writer = new BufferedWriter(new OutputStreamWriter() {
				new FileOutputStream(workingDirectory +"\\@RESULTS.txt"), "utf-8"))) {

				writer.write("Query weights:\n");
				writer.write("Title weight --> " + titleWeight + "\n");
				writer.write("Objective weight --> " + objectiveWeight + "\n");
				writer.write("Category weight --> " + callWeight + "\n\n");

				for (int i =0; i < queryList.getLength(); i++){ // For every query found in the queries xml file.

					// Search for the queries terms.
					// Empty the lists because there are multiple queries. Delete the previous results from the program.
					titleHitsList.clear();
					objectiveHitsList.clear();
					mergedList.clear();

					//Get the appropriate tags from the queries file.
					title = queries.getElementsByTagName("title").item(i).getTextContent();
					objective = queries.getElementsByTagName("objective").item(i).getTextContent();
					identifier = queries.getElementsByTagName("identifier").item(i).getTextContent();

					titleHitsList = searchField(analyzer, searcher, hitsPerPage, "title", title);
					objectiveHitsList = searchField(analyzer, searcher, hitsPerPage, "objective", objective);

					// Put in merged list.
					mergedList.putAll(objectiveHitsList); // Put all the documents from titleHitsList in the new list.
					mergedList.putAll(titleHitsList); // Put all the documents form objectiveHitsList in the new list.

					System.out.println("MERGED LIST: "+mergedList);
					System.out.println("---------------------");

					writer.write("**********\n");
					writer.write("QUERY "+ (i+1) + "\n");
					writer.write("**********\n");

					Iterator entries = mergedList.entrySet().iterator(); // Parse the merged list.

					while (entries.hasNext()) { // For every element.

						Map.Entry entry = (Map.Entry) entries.next();
						Integer key = (Integer)entry.getKey();
						Float value = (Float)entry.getValue();
						System.out.println("Key = " + key + ", Value = " + value);

						identifierHit = 0;
						Document d = searcher.doc(key);

						if (d.get("identifier").equalsIgnoreCase(identifier)) {

							identifierHit = 1; // If the identifier is the same with the one searched for.
							System.out.println("IDENTIFIER  HIT");
						}

						calculateScore(key, titleWeight, objectiveWeight, callWeight, identifierHit, searcher);
						// Calculate the final score of the element.
					}

					System.out.println("Merged list: "+mergedList);
					System.out.println("Merged list size: "+mergedList.size());

					//Sort the results based on the final score.
					LinkedHashMap<Integer, Float> sorted = new LinkedHashMap<>(); // Create a linked list for the
					// sorting procedure.

					java.util.List<Map.Entry<Integer, Float>> temp = new ArrayList<>(mergedList.entrySet());
					Collections.sort(temp, new Comparator<Entry<Integer, Float>>() {
						@Override
						public int compare(
								Entry<Integer, Float> o1, Entry<Integer, Float> o2) {
							return o2.getValue().compareTo(o1.getValue());
						}
					});

					Map<Integer, Float> sortedMap = new LinkedHashMap<>();

					for (Map.Entry<Integer, Float> entry : temp) {

						sortedMap.put(entry.getKey(), entry.getValue()); // Put the element in the liked map.
					}

					// Export the top numberOfResultsToShow items.
					Iterator it = sortedMap.entrySet().iterator();
					counter = 0;

					while (it.hasNext()) {

							Map.Entry entry = (Map.Entry) it.next();
							Integer key = (Integer) entry.getKey();

						if (counter < numberOfResultsToShow) {
							writer.write("-------------------------------------------\n");
							writer.write("("+(counter + 1)+")\n");
							writer.write("*SCORE --> " + entry.getValue() + "\n");
							writer.write("*TITLE --> " + searcher.doc(key).get("title") + "\n");
							writer.write("*ABSTRACT --> " + searcher.doc(key).get("objective") + "\n\n");
							counter++;

						}

					}

					//***DEBUG WRITES IN FILE***
					//writer.write("TITLE LIST: "+titleHitsList.toString()+"\n\n");
					//writer.write("OBJECTIVE LIST: "+objectiveHitsList.toString()+"\n\n");
					//writer.write("MERGED LIST: "+mergedList.toString()+"\n\n");
					//writer.write("SORTED BY SCORE: "+sortedMap.toString()+"\n");

					if (i == queryList.getLength() - 1){ // In the last query.

						writer.write("-------------------------------------------\n");
						writer.write("END OF RESULTS...\n");
						writer.write("-------------------------------------------\n\n");

					} else { // In every other query.

						writer.write("-------------------------------------------\n");
						writer.write("NEXT QUERY...\n");
						writer.write("-------------------------------------------\n\n");

					}

				}

			} catch (Exception e){

				e.printStackTrace();
			}

		} catch (Exception e) {

			e.printStackTrace();
		}

		try {

			reader.close(); // Close the reader.

		} catch(Exception p){

			p.printStackTrace();

		}

	}

}
