package controllers;

import highscore.Failure;
import highscore.PublishHighScoreEndpoint;
import highscore.PublishHighScoreService;
import highscore.data.GenderType;
import highscore.data.HighScoreRequestType;
import models.Category;
import models.JeopardyDAO;
import models.JeopardyGame;
import models.JeopardyUser;
import play.Logger;
import play.cache.Cache;
import play.data.DynamicForm;
import play.data.DynamicForm.Dynamic;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import twitter.ITwitterClient;
import twitter.TwitterCli;
import twitter.TwitterStatusMessage;
import views.html.jeopardy;
import views.html.question;
import views.html.winner;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

@Security.Authenticated(Secured.class)
public class GameController extends Controller {
	
	protected static final int CATEGORY_LIMIT = 5;
	
	@Transactional
	public static Result index() {
		return redirect(routes.GameController.playGame());
	}
	
	@play.db.jpa.Transactional(readOnly = true)
	private static JeopardyGame createNewGame(String userName) {
		return createNewGame(JeopardyDAO.INSTANCE.findByUserName(userName));
	}
	
	@play.db.jpa.Transactional(readOnly = true)
	private static JeopardyGame createNewGame(JeopardyUser user) {
		if(user == null) // name still stored in session, but database dropped
			return null;

		Logger.info("[" + user + "] Creating a new game.");
		List<Category> allCategories = JeopardyDAO.INSTANCE.findEntities(Category.class);
		
		if(allCategories.size() > CATEGORY_LIMIT) {
			// select 5 categories randomly (simple)
			Collections.shuffle(allCategories);
			allCategories = allCategories.subList(0, CATEGORY_LIMIT);
		}
		Logger.info("Start game with " + allCategories.size() + " categories.");
		JeopardyGame game = new JeopardyGame(user, allCategories);
		cacheGame(game);
		return game;
	}
	
	private static void cacheGame(JeopardyGame game) {
		Cache.set(gameId(), game, 3600);
	}
	
	private static JeopardyGame cachedGame(String userName) {
		Object game = Cache.get(gameId());
		if(game instanceof JeopardyGame)
			return (JeopardyGame) game;
		return createNewGame(userName);
	}
	
	private static String gameId() {
		return "game." + uuid();
	}

	private static String uuid() {
		String uuid = session("uuid");
		if (uuid == null) {
			uuid = UUID.randomUUID().toString();
			session("uuid", uuid);
		}
		return uuid;
	}
	
	@Transactional
	public static Result newGame() {
		Logger.info("[" + request().username() + "] Start new game.");
		JeopardyGame game = createNewGame(request().username());
		return ok(jeopardy.render(game));
	}
	
	@Transactional
	public static Result playGame() {
		Logger.info("[" + request().username() + "] Play the game.");
		JeopardyGame game = cachedGame(request().username());
		if(game == null) // e.g., username still in session, but db dropped
			return redirect(routes.Authentication.login());
		if(game.isAnswerPending()) {
			Logger.info("[" + request().username() + "] Answer pending... redirect");
			return ok(question.render(game));
		} else if(game.isGameOver()) {
			Logger.info("[" + request().username() + "] Game over... redirect");

			String uuid = null;

			try {
				uuid = doPublishHighScore(game);
				doTwitterStatus(game.getWinner().getUser().getName(), uuid);
			} catch (Exception e) {
				uuid = null;
			}

			return ok(winner.render(game, uuid));
		}			
		return ok(jeopardy.render(game));
	}
	
	@play.db.jpa.Transactional(readOnly = true)
	public static Result questionSelected() {
		JeopardyGame game = cachedGame(request().username());
		if(game == null || !game.isRoundStart())
			return redirect(routes.GameController.playGame());
		
		Logger.info("[" + request().username() + "] Questions selected.");		
		DynamicForm form = Form.form().bindFromRequest();
		
		String questionSelection = form.get("question_selection");
		
		if(questionSelection == null || questionSelection.equals("") || !game.isRoundStart()) {
			return badRequest(jeopardy.render(game));
		}
		
		game.chooseHumanQuestion(Long.parseLong(questionSelection));
		
		return ok(question.render(game));
	}
	
	@play.db.jpa.Transactional(readOnly = true)
	public static Result submitAnswers() {
		JeopardyGame game = cachedGame(request().username());
		if(game == null || !game.isAnswerPending())
			return redirect(routes.GameController.playGame());
		
		Logger.info("[" + request().username() + "] Answers submitted.");
		Dynamic form = Form.form().bindFromRequest().get();
		
		@SuppressWarnings("unchecked")
		Map<String,String> data = form.getData();
		List<Long> answerIds = new ArrayList<>();
		
		for(String key : data.keySet()) {
			if(key.startsWith("answers[")) {
				answerIds.add(Long.parseLong(data.get(key)));
			}
		}
		game.answerHumanQuestion(answerIds);
		if(game.isGameOver()) {
			return redirect(routes.GameController.gameOver());
		} else {
			return ok(jeopardy.render(game));
		}
	}
	
	@play.db.jpa.Transactional(readOnly = true)
	public static Result gameOver() {
		JeopardyGame game = cachedGame(request().username());
		if(game == null || !game.isGameOver())
			return redirect(routes.GameController.playGame());
		
		Logger.info("[" + request().username() + "] Game over.");

		String uuid = "";

		try {
			uuid = doPublishHighScore(game);
			doTwitterStatus(game.getWinner().getUser().getName(), uuid);
		} catch (Exception e) {

			uuid = null;
		}

		return ok(winner.render(game, uuid));
	}

	private static String doPublishHighScore(JeopardyGame game) throws Exception {
		PublishHighScoreService hsService = new PublishHighScoreService();
		PublishHighScoreEndpoint hsEndpoint = hsService.getPublishHighScorePort();
		HighScoreRequestType hsRequestType = new HighScoreRequestType();
		hsRequestType.setUserKey("3ke93-gue34-dkeu9");

		highscore.data.UserType winner = new highscore.data.UserType();
		highscore.data.UserType loser = new highscore.data.UserType();

		JeopardyUser jeopardyWinner = game.getWinner().getUser();
		JeopardyUser jeopardyLoser = game.getLoser().getUser();

		winner.setFirstName(jeopardyWinner.getFirstName());
		winner.setLastName(jeopardyWinner.getLastName());
		winner.setPassword("");
		winner.setPoints(game.getWinner().getProfit());
		if(jeopardyWinner.getGender() == JeopardyUser.Gender.male)
			winner.setGender(GenderType.MALE);
		else
			winner.setGender(GenderType.FEMALE);

		loser.setFirstName(jeopardyLoser.getFirstName());
		loser.setLastName(jeopardyLoser.getLastName());
		loser.setPassword("");
		loser.setPoints(game.getLoser().getProfit());
		if(jeopardyLoser.getGender() == JeopardyUser.Gender.male)
			loser.setGender(GenderType.MALE);
		else
			loser.setGender(GenderType.FEMALE);

		GregorianCalendar date = new GregorianCalendar();
		XMLGregorianCalendar xmlc;
		try {
			xmlc = DatatypeFactory.newInstance().newXMLGregorianCalendar();

			date.setTime(jeopardyWinner.getBirthDate());
			xmlc.setYear(date.get(Calendar.YEAR));
			xmlc.setMonth(date.get(Calendar.MONTH) + 1);
			xmlc.setDay(date.get(Calendar.DAY_OF_MONTH));
			winner.setBirthDate(xmlc);

			date.setTime(jeopardyLoser.getBirthDate());
			xmlc.setYear(date.get(Calendar.YEAR));
			xmlc.setMonth(date.get(Calendar.MONTH) + 1);
			xmlc.setDay(date.get(Calendar.DAY_OF_MONTH));
			loser.setBirthDate(xmlc);

		} catch (DatatypeConfigurationException ex) {
			Logger.error("DatatypeConfigurationException",ex);
		}

		highscore.data.UserDataType users = new highscore.data.UserDataType();
		users.setWinner(winner);
		users.setLoser(loser);

		hsRequestType.setUserData(users);

		try {
			String hsUUID = hsEndpoint.publishHighScore(hsRequestType);
			Logger.info("Publish Highscore UUID: " + hsUUID);
			return hsUUID;
		} catch (Failure ex) {
			Logger.error("Publish Highscore Error", ex);
			ex.printStackTrace();
		}
		return null;

	}

	private static void  doTwitterStatus(String from, String uuid) {

		if(uuid == null || uuid.isEmpty())
			return;
		if(from == null || from.isEmpty())
			return;

		TwitterStatusMessage twitter = new TwitterStatusMessage(from, uuid, new Date());
		ITwitterClient client = new TwitterCli();
		try {
			client.publishUuid(twitter);
			Logger.info("Twitter Status Message UUID: " + uuid);
		} catch (Exception ex) {
			Logger.error("Twitter Status Message Error", ex);
		}
	}
}
