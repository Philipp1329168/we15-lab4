import at.ac.tuwien.big.we.dbpedia.api.DBPediaService;
import at.ac.tuwien.big.we.dbpedia.api.SelectQueryBuilder;
import at.ac.tuwien.big.we.dbpedia.vocabulary.DBPedia;
import at.ac.tuwien.big.we.dbpedia.vocabulary.DBPediaOWL;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import data.JSONDataInserter;
import models.Answer;
import models.Category;
import models.JeopardyDAO;
import models.Question;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.libs.F.Function0;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


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

		Category category = new Category();
		category.setNameDE("Filme");
		category.setNameEN("Films");

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

		Question question = new Question();
		question.setTextDE("Filme mit Tim Burton");
		question.setTextEN("Films with Tim Burton");
		question.setCategory(category);
		question.setValue(50);

		List<Answer> listAnswer = new ArrayList<>();
		//add wrong answers
		for(int i=0;i<germanNoTimBurtonMovieNames.size();i++) {
			Answer answer = new Answer();
			answer.setCorrectAnswer(false);
			answer.setTextDE(germanNoTimBurtonMovieNames.get(i));
			answer.setTextEN(englishNoTimBurtonMovieNames.get(i));
			answer.setQuestion(question);
			JeopardyDAO.INSTANCE.persist(answer);
		}
		//add correct answers
		for(int i=0;i<germanTimBurtonMovieNames.size();i++) {
			Answer answer = new Answer();
			answer.setCorrectAnswer(true);
			answer.setTextDE(germanNoTimBurtonMovieNames.get(i));
			answer.setTextEN(englishNoTimBurtonMovieNames.get(i));
			answer.setQuestion(question);
			JeopardyDAO.INSTANCE.persist(answer);
		}

		question.setAnswers(listAnswer);
		JeopardyDAO.INSTANCE.persist(question);
		JeopardyDAO.INSTANCE.persist(category);

		Logger.info(" category from DBPedia '" + category.getNameDE() + "' inserted.");

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