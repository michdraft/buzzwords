package com.taboozle;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author The Taboozle Team
 *
 * The Game class is a database abstraction class that deals with the database
 * transaction necessary to care a game of taboozle forward. Game should only
 * be used by game manager as a matter of design.
 */
public class Game extends SQLiteOpenHelper
{

  /**
   * logging tag
   */
  public static String TAG = "Game";

  /**
   * The current context (used for database creation and initialization)
   */
  private final Context curContext;

  /**
   * The list of cardIds that we pull from (our "deck" of cards)
   */
  private ArrayList<Long> cardIds;

  /**
   * The position in the list of card ids (where we are in the "deck")
   */
  private int cardIdPosition;

  /**
   * Standard constructor. If you're wondering about the necessity of the
   * context, it is used to create the database of the superclass and we need
   * it to populate the card table with the xml resource containing the starter
   * pack.
   * @param context - the context to pass to the superclass and initialize the
   * card table.
   */
  public Game( Context context )
  {
    super( context, GameData.DATABASE_NAME, null,
           GameData.DATABASE_VERSION );
    Log.d( TAG, "Game()" );    
    this.curContext = context;
  }

  /**
   * Query the database for all the cards it has. That query specifies a random
   * order; thus, a cursor full of longs is returned. We push those longs into
   * our newly initialized ArrayList, cardIds. Note the cardIdPosition is set
   * to zero indicating the first card id in our "deck."
   */
  public void prepDeck()
  {
    Log.d( TAG, "prepDeck()" );
    // initialize our data structures
    this.cardIds = new ArrayList<Long>();
    this.cardIdPosition = 0;

    // query for ids
    SQLiteDatabase db = this.getReadableDatabase();
    String[] columns = new String[] {GameData.Cards._ID};
    Cursor cur = db.query( GameData.CARD_TABLE_NAME, columns, null, null,
                         null, null, "RANDOM()");

    // iterate through the cursor pushing the ids into cardIds
    if( cur.moveToFirst() )
    {
      do
      {
        int idColumn = cur.getColumnIndex( GameData.Cards._ID );
        this.cardIds.add( cur.getLong( idColumn ) );

      } while( cur.moveToNext() );
    }
    cur.close();
  }

  /**
   * Query the database for all round scores for a team in a given game.
   *
   * @param teamID - the database ID of the team whose scores are being retrieved
   * @param gameID - the database ID of the game whose scores are being retrieved
   * @return an array of scores, one element per round
   */
  public long[] getRoundScores(long teamID, long gameID)
  {
    Log.d( TAG, "getRoundScores()" );
    // query for scores
    SQLiteDatabase db = this.getReadableDatabase();
    String[] columns = new String[] {GameData.TurnScores.SCORE};
    Cursor cur = db.query( GameData.TURN_SCORES_TABLE_NAME, columns,
    		               GameData.TurnScores.TEAM_ID + "=" + teamID + " AND " +
    		               GameData.TurnScores.GAME_ID + "=" + gameID, null, null, null,
    		               GameData.TurnScores.ROUND);

    long[] scores = new long[cur.getCount()];

    // iterate through the cursor populating array of scores
    if( cur.moveToFirst() )
    {
      int i = 0;
      do
      {
        int scoreColumn = cur.getColumnIndex( GameData.TurnScores.SCORE );
        scores[i] = cur.getLong( scoreColumn );
        i++;
      } while( cur.moveToNext() );
    }

    cur.close();
    return scores;
  }

  /**
   * Get the card indicated by the cardIdPosition. If we've dealt past the end
   * of the deck, we should prep the deck.
   * @return the card we want
   */
  public Card getNextCard()
  {
    Log.d( TAG, "getNextCard()" );
    // check deck bounds
    if( this.cardIdPosition >= this.cardIds.size() )
    {
      this.prepDeck();
    }

    // query for the specific card indicated by cardId
    SQLiteDatabase db = this.getReadableDatabase();
    String[] columns = new String[] {GameData.Cards._ID, GameData.Cards.TITLE,
                        GameData.Cards.BAD_WORDS};
    Cursor cur = db.query( GameData.CARD_TABLE_NAME, columns,
                           GameData.Cards._ID + "=" +
                           this.cardIds.get( this.cardIdPosition++ ),
                           null, null, null, null, "1" );

    // create a blank card
    Card card = new Card();

    // Get the first result from the cursor and populate the blank card
    if( cur.moveToFirst() )
    {
      int idColumn = cur.getColumnIndex( GameData.Cards._ID );
      int titleColumn = cur.getColumnIndex( GameData.Cards.TITLE );
      int badWordsColumn = cur.getColumnIndex( GameData.Cards.BAD_WORDS );

      card.setId( cur.getLong( idColumn ) );
      card.setTitle( cur.getString( titleColumn ) );
      card.setBadWords( cur.getString( badWordsColumn ) );
    }

    // release the cursor
    cur.close();

    // return the card (it could be blank)
    return card;
  }

  /**
   * Create a game identified by the current date and return its database id
   * @return the id of the newly created game
   */
  public long newGame()
  {
    Log.d( TAG, "newGame()" );
    // Prepare the current date for insertion
    Date currentTime = new Date();
    String dateString = currentTime.toString();
    ContentValues values = new ContentValues();
    values.put( GameData.Games.TIME, dateString );
    SQLiteDatabase db = this.getWritableDatabase();

    // Do the insert and return the row id
    return db.insert(GameData.GAME_TABLE_NAME, "", values);
  }

  /**
   * Create a team identified by name and return the team's id
   * @param name - the name of the team
   * @return the id of the newly created team
   */
  public long newTeam( String name )
  {
    Log.d( TAG, "newTeam()" );
    ContentValues values = new ContentValues();
    values.put(GameData.Teams.NAME, name);
    SQLiteDatabase db = this.getWritableDatabase();
    return db.insert(GameData.TEAM_TABLE_NAME, "", values);
  }
  /**
   * Creates an entry in the Game History table for each team representing the
   * game's final scores.
   */
  public long completeGame( long gameId, long teamId, long score )
  {
    Log.d( TAG, "completeGame()" );
    ContentValues values = new ContentValues();
    values.put( GameData.FinalScores.GAME_ID, gameId );
    values.put( GameData.FinalScores.TEAM_ID, teamId );
    values.put( GameData.FinalScores.SCORE, score );
    SQLiteDatabase db = this.getWritableDatabase();
    return db.insert( GameData.FINAL_SCORES_TABLE_NAME, "", values );
  }
  
  /**
   * Creates a turn record in the database. The turn knows about the game,
   * team, and round in which it occurred, as well as its score.
   * @param gameId - the id of the game in which the turn took place
   * @param teamId - the id of the team doing the guessing on the turn
   * @param round - the round in which the turn took place
   * @param score - the score for the turn
   * @return the id of the turn
   */
  public long newTurn( long gameId, long teamId, long round, long score )
  {
    Log.d( TAG, "newTurn()" );
    ContentValues values = new ContentValues();
    values.put( GameData.TurnScores.GAME_ID, gameId );
    values.put( GameData.TurnScores.TEAM_ID, teamId );
    values.put( GameData.TurnScores.ROUND, round );
    values.put( GameData.TurnScores.SCORE, score );
    SQLiteDatabase db = this.getWritableDatabase();
    return db.insert(GameData.TURN_SCORES_TABLE_NAME, "", values);
  }

  /**
   * Adds a record of a played card to the database
   * @param gameId - the game the card occurred in
   * @param teamId - the team calling the card
   * @param cardId - the card played
   * @param turnScoreId - the turn the card was called in
   * @param rws - whether the card was right, wrong or skipped
   */
  public void completeCard( long gameId, long teamId, long cardId,
		  					long turnScoreId, long rws )
  {
    Log.d( TAG, "completeCard()" );
    ContentValues values = new ContentValues();
    values.put(  GameData.GameHistory.GAME_ID, gameId );
    values.put( GameData.GameHistory.TEAM_ID, teamId );
    values.put( GameData.GameHistory.CARD_ID, cardId );
    values.put( GameData.GameHistory.TURN_SCORE_ID, turnScoreId );
    values.put( GameData.GameHistory.RWS, rws );
    SQLiteDatabase db = this.getWritableDatabase();
    db.insert( GameData.GAME_HISTORY_TABLE_NAME, "", values );
  }


  /*
   * Most Skips
   * 
   * SELECT team_id
   * FROM gamehistory
   * WHERE game_id = gameID
   * GROUP BY team_id
   * HAVING RWS = 2
   * ORDER BY COUNT(*) DESC
   * LIMIT 1;
   * 
   */
  public String[] awardsQuery( int awardID, long gameID, int turnTime)
  {
   	String[] results = {"", ""}; //TeamID, AwardValue or Word
  	SQLiteDatabase db = this.getReadableDatabase();
  	Cursor cursor = db.rawQuery( "select 'DEFAULT', 'DEFAULT'", null);
  	
  	switch (awardID) 
  	{  	
  	  case 1: //Most Skips 	  
      	cursor = db.rawQuery("SELECT " + GameData.GameHistory.TEAM_ID + ", COUNT(*)" +  
      		" FROM " + GameData.GAME_HISTORY_TABLE_NAME + " " +
      		" WHERE " + GameData.GameHistory.GAME_ID + "=" + gameID +
      		   " and " + GameData.GameHistory.RWS + "=2 " + 
      		" GROUP BY " + GameData.GameHistory.TEAM_ID + " " + 
      		" ORDER BY 2 DESC" + 
      		" LIMIT 2", null);
      	break;
  	  case 2: //Most Incorrect
        cursor = db.rawQuery("SELECT " + GameData.GameHistory.TEAM_ID + ", COUNT(*)" +  
            " FROM " + GameData.GAME_HISTORY_TABLE_NAME +
            " WHERE " + GameData.GameHistory.GAME_ID + "=" + gameID +
               " and " + GameData.GameHistory.RWS + "=1" + 
            " GROUP BY " + GameData.GameHistory.TEAM_ID + 
            " ORDER BY 2 DESC" + 
            " LIMIT 2", null);
        break;
  	  case 3: //Highest single turn score    
  	    cursor = db.rawQuery("SELECT " + GameData.TurnScores.TEAM_ID + ", " + GameData.TurnScores.SCORE +
            " FROM " + GameData.TURN_SCORES_TABLE_NAME +
            " WHERE " + GameData.TurnScores.GAME_ID + "=" + gameID + 
            " GROUP BY " + GameData.TurnScores.TEAM_ID + 
            " ORDER BY " + GameData.TurnScores.SCORE + " DESC" + 
            " LIMIT 2", null);
  	    break;
  	}
  	
  	// Set results array to the answers returned from sqlite query
    if (cursor.moveToFirst())
    {
      results[0] = cursor.getString(0);
      
      if (cursor.getColumnCount() == 2)
        results[1] = cursor.getString(1);
      else
        results[1] = "EMPTY";
      
    }    
    // Empty results get set to -1
    else 
    {
      results[0] = "EMPTY";
      results[1] = "EMPTY";
    }
        
    //Handle Tied Awards
    if (cursor.moveToNext())
    {
      if (cursor.getColumnCount() == 2)
      {
        if (cursor.getString(1).compareTo(results[1]) == 0)  //Ties disqualify award
        {
          results[0] = "TIE";
          results[1] = cursor.getString(1);
        }
      }
    }
    
    cursor.close();
  	return results;
  }
  
  /* (non-Javadoc)
   * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
   */
  @Override
  public void onCreate( SQLiteDatabase db )
  {
    Log.d( TAG, "onCreate()" );
    db.execSQL( "CREATE TABLE " + GameData.TEAM_TABLE_NAME + " (" +
                GameData.Teams._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                GameData.Teams.NAME + " TEXT);" );
    db.execSQL( "CREATE TABLE " + GameData.GAME_TABLE_NAME + " (" +
                GameData.Games._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                GameData.Games.TIME + " TEXT);" );
    db.execSQL( "CREATE TABLE " + GameData.TURN_SCORES_TABLE_NAME + " (" +
                GameData.TurnScores._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                GameData.TurnScores.TEAM_ID + " INTEGER," +
                GameData.TurnScores.GAME_ID + " INTEGER," +
                GameData.TurnScores.ROUND + " INTEGER," +
                GameData.TurnScores.SCORE + " INTEGER);" );
    db.execSQL( "CREATE TABLE " + GameData.FINAL_SCORES_TABLE_NAME + " (" +
                GameData.FinalScores._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                GameData.FinalScores.TEAM_ID + " INTEGER," +
                GameData.FinalScores.GAME_ID + " INTEGER," +
                GameData.FinalScores.SCORE + " INTEGER);");
    db.execSQL( "CREATE TABLE " + GameData.GAME_HISTORY_TABLE_NAME + " (" +
                GameData.GameHistory._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                GameData.GameHistory.TEAM_ID + " INTEGER," +
                GameData.GameHistory.GAME_ID + " INTEGER," +
                GameData.GameHistory.CARD_ID + " INTEGER," +
                GameData.GameHistory.TURN_SCORE_ID + " INTEGER," +
                GameData.GameHistory.RWS + " INTEGER);" );
    db.execSQL( "CREATE TABLE " + GameData.CARD_TABLE_NAME + " (" +
                GameData.Cards._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                GameData.Cards.PACK_NAME + " TEXT," +
                GameData.Cards.TITLE + " TEXT," +
                GameData.Cards.BAD_WORDS + " TEXT," +
                GameData.Cards.CATEGORIES + " TEXT);" );

    InputStream starterXML =
      curContext.getResources().openRawResource(R.raw.starter);
    DocumentBuilderFactory docBuilderFactory =
      DocumentBuilderFactory.newInstance();
    try
    {
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(starterXML);
      NodeList cardNodes = doc.getElementsByTagName( "card" );
      for(int i = 0; i < cardNodes.getLength(); i++)
      {
        NodeList titleWhiteAndBads = cardNodes.item( i ).getChildNodes();
        Node titleNode = null;
        Node badsNode = null;
        Node categoriesNode = null;
        Node packNameNode = null;
        for( int j = 0; j < titleWhiteAndBads.getLength(); j++ )
        {
          String candidateName = titleWhiteAndBads.item( j ).getNodeName();
          if( candidateName.equals( "title" ) )
          {
            titleNode = titleWhiteAndBads.item( j );
          }
          else if( candidateName.equals( "pack-name" ) )
          {
            packNameNode = titleWhiteAndBads.item( j );
          }
          else if( candidateName.equals( "bad-words" ) )
          {
            badsNode = titleWhiteAndBads.item( j );
          }
          else if( candidateName.equals( "categories" ) )
          {
            categoriesNode = titleWhiteAndBads.item( j );
          }
          else
          {
            continue; // We found some #text
          }
        }
        String title = titleNode.getFirstChild().getNodeValue();
        String packName = packNameNode.getFirstChild().getNodeValue();
        String categories = categoriesNode.getFirstChild().getNodeValue();
        String badWords = "";
        NodeList bads = badsNode.getChildNodes();
        for( int j = 0; j < bads.getLength(); j++ )
        {
          String candidateName = bads.item( j ).getNodeName();
          if( candidateName.equals( "word" ) )
          {
            badWords += bads.item( j ).getFirstChild().getNodeValue() + ",";
          }
        }
        // hack because I have a comma at the end
        badWords = badWords.substring( 0, badWords.length() - 1 );

        // use a hard-coded query for performance and readability
        db.execSQL( "INSERT INTO " + GameData.CARD_TABLE_NAME + " (" +
                    GameData.Cards.PACK_NAME + "," + GameData.Cards.TITLE  + ", " +
                    GameData.Cards.BAD_WORDS + ", " + GameData.Cards.CATEGORIES +
                    ") VALUES (\"" +
                    packName + "\",\"" +
                    title + "\",\"" +
                    badWords + "\",\"" +
                    categories + "\");" );
      }
    }
    catch( ParserConfigurationException e )
    {
      e.printStackTrace();
    }
    catch( SAXException e )
    {
      e.printStackTrace();
    }
    catch( IOException e )
    {
      e.printStackTrace();
    }
  }

  /* (non-Javadoc)
   * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
   */
  @Override
  public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion )
  {
    Log.d( TAG, "onUpgrade()" );
    Log.w( TAG, "Upgrading database from version " + oldVersion + " to "
           + newVersion + ", which will destroy all old data" );
    db.execSQL( "DROP TABLE IF EXISTS" + GameData.TEAM_TABLE_NAME + ";" );
    db.execSQL( "DROP TABLE IF EXISTS" + GameData.GAME_TABLE_NAME + ";" );
    db.execSQL( "DROP TABLE IF EXISTS" + GameData.TURN_SCORES_TABLE_NAME + ";" );
    db.execSQL( "DROP TABLE IF EXISTS" + GameData.FINAL_SCORES_TABLE_NAME + ";" );
    db.execSQL( "DROP TABLE IF EXISTS" + GameData.GAME_HISTORY_TABLE_NAME + ";" );
    db.execSQL( "DROP TABLE IF EXISTS" + GameData.CARD_TABLE_NAME + ";" );
    onCreate( db );
  }
  
}
