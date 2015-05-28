import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import at.ac.tuwien.big.we.dbpedia.api.DBPediaService;
import at.ac.tuwien.big.we.dbpedia.api.SelectQueryBuilder;
import at.ac.tuwien.big.we.dbpedia.vocabulary.DBPedia;
import at.ac.tuwien.big.we.dbpedia.vocabulary.DBPediaOWL;
import com.hp.hpl.jena.rdf.model.Resource;
import models.Category;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.libs.F.Function0;
import data.JSONDataInserter;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;


public class Global extends GlobalSettings {
	
	@play.db.jpa.Transactional
	public static void insertJSonData() throws IOException {
		String file = Play.application().configuration().getString("questions.filePath");		
		Logger.info("Data from: " + file);
		InputStream is = Play.application().resourceAsStream(file);
		List<Category> categories = JSONDataInserter.insertData(is);
		Logger.info(categories.size() + " categories from json file '" + file + "' inserted.");
	}

	@play.db.jpa.Transactional
	public static void insertOpenLinkedData()  {

		// Check if DBpedia is available
		if(!DBPediaService.isAvailable())
			return;
// Resource Tim Burton is available at http://dbpedia.org/resource/Tim_Burton
// Load all statements as we need to get the name later
		Resource director = DBPediaService.loadStatements(DBPedia.createResource("Tim_Burton"));
// Resource Johnny Depp is available at http://dbpedia.org/resource/Johnny_Depp
// Load all statements as we need to get the name later
		Resource actor = DBPediaService.loadStatements(DBPedia.createResource("Johnny_Depp"));
// retrieve english and german names, might be used for question text
		String englishDirectorName = DBPediaService.getResourceName(director, Locale.ENGLISH);
		String germanDirectorName = DBPediaService.getResourceName(director, Locale.GERMAN);
		String englishActorName = DBPediaService.getResourceName(actor, Locale.ENGLISH);
		String germanActorName = DBPediaService.getResourceName(actor, Locale.GERMAN);
// build SPARQL-query
		SelectQueryBuilder movieQuery = DBPediaService.createQueryBuilder()
				.setLimit(5) // at most five statements
				.addWhereClause(RDF.type, DBPediaOWL.Film)
				.addPredicateExistsClause(FOAF.name)
				.addWhereClause(DBPediaOWL.director, director)
				.addFilterClause(RDFS.label, Locale.GERMAN)
				.addFilterClause(RDFS.label, Locale.ENGLISH);
// retrieve data from dbpedia
		Model timBurtonMovies = DBPediaService.loadStatements(movieQuery.toQueryString());
// get english and german movie names, e.g., for right choices
		List<String> englishTimBurtonMovieNames =
				DBPediaService.getResourceNames(timBurtonMovies, Locale.ENGLISH);
		List<String> germanTimBurtonMovieNames =
				DBPediaService.getResourceNames(timBurtonMovies, Locale.GERMAN);
// alter query to get movies without tim burton
		movieQuery.removeWhereClause(DBPediaOWL.director, director);
		movieQuery.addMinusClause(DBPediaOWL.director, director);
// retrieve data from dbpedia
		Model noTimBurtonMovies = DBPediaService.loadStatements(movieQuery.toQueryString());
// get english and german movie names, e.g., for wrong choices
		List<String> englishNoTimBurtonMovieNames =
				DBPediaService.getResourceNames(noTimBurtonMovies, Locale.ENGLISH);
		List<String> germanNoTimBurtonMovieNames =
				DBPediaService.getResourceNames(noTimBurtonMovies, Locale.GERMAN);

		for (String string : englishNoTimBurtonMovieNames) {
			System.out.println(string);
		}

		for (String string : germanNoTimBurtonMovieNames) {
			System.out.println(string);
		}

	}
	
	@play.db.jpa.Transactional
    public void onStart(Application app) {
       try {
    	   JPA.withTransaction(new Function0<Boolean>() {

			@Override
			public Boolean apply() throws Throwable {
				insertJSonData();
				insertOpenLinkedData();
				return true;
			}
			   
			});
       } catch (Throwable e) {
    	   e.printStackTrace();
       }
    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }

}