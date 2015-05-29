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

		if(!DBPediaService.isAvailable())
			return;

		Category category = new Category();
		category.setNameDE("Filme");
		category.setNameEN("Films");

		ArrayList<String> persons = new ArrayList<>();
		persons.add("Tom_Cruise");
		persons.add("Leonardo_DiCaprio");
		persons.add("Megan_Fox");
		persons.add("Brad_Pitt");
		persons.add("Mila_Kunis");

		for(String person : persons) {
			Resource actor = DBPediaService.loadStatements(DBPedia.createResource(person));

			SelectQueryBuilder movieQuery = DBPediaService.createQueryBuilder()
					.setLimit(5)
					.addWhereClause(RDF.type, DBPediaOWL.Film)
					.addPredicateExistsClause(FOAF.name)
					.addPredicateExistsClause(RDFS.label)
					.addWhereClause(DBPediaOWL.starring, actor)
					.addFilterClause(RDFS.label, Locale.ENGLISH)
					.addFilterClause(RDFS.label, Locale.GERMAN);

			Model actorMovies = DBPediaService.loadStatements(movieQuery.toQueryString());
			List<String> englishActorMovieNames = DBPediaService.getResourceNames(actorMovies, Locale.ENGLISH);
			List<String> germanActorMovieNames = DBPediaService.getResourceNames(actorMovies, Locale.GERMAN);

			movieQuery.removeWhereClause(DBPediaOWL.starring, actor);
			movieQuery.addMinusClause(DBPediaOWL.starring, actor);
			movieQuery.setLimit(100);

			Model noActorMovies = DBPediaService.loadStatements(movieQuery.toQueryString());
			List<String> englishNoActorMovieNames = DBPediaService.getResourceNames(noActorMovies, Locale.ENGLISH);
			List<String> germanNoActorMovieNames = DBPediaService.getResourceNames(noActorMovies, Locale.GERMAN);

			Question q = new Question();
			q.setTextDE("In welchem Filmen ist" + DBPediaService.getResourceName(actor,Locale.GERMAN ) + " ein Hauptdarsteller");
			q.setTextEN("In which films is " + DBPediaService.getResourceName(actor,Locale.ENGLISH ) + " a leading actor");

			int right = (int) (Math.random() * 3) + 1;
			for (int i = 0; i < right; i++) {
				Answer a = new Answer();
				a.setTextDE(germanActorMovieNames.get(i));
				a.setTextEN(englishActorMovieNames.get(i));
				q.addRightAnswer(a);
			}
			for (int i = right; i < 6; i++) {
				Answer a = new Answer();
				int random = (int) (Math.random() * 99);
				a.setTextDE(germanNoActorMovieNames.get(random));
				a.setTextEN(englishNoActorMovieNames.get(random));
				q.addWrongAnswer(a);
			}
			q.setCategory(category);
			q.setValue(category.getQuestions().size()*10 + 10);

			JeopardyDAO.INSTANCE.persist(q);
			category.addQuestion(q);
		}
		JeopardyDAO.INSTANCE.persist(category);
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