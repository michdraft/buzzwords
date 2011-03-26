package com.wordfrenzy;

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

import com.wordfrenzy.R;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author The WordFrenzy Team
 *
 * The Game class is a database abstraction class that deals with the database
 * transaction necessary to care a game of wordfrenzy forward. Game should only
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
  private ArrayList<Card> deck;

  /**
   * The position in the list of card ids (where we are in the "deck")
   */
  private int cardPosition;

  
  /**
   * @return the cardPosition
   */
  public int getCardPosition()
  {
    return this.cardPosition;
  }
  
  /**
   * @param cardPosition the cardPosition to set
   */
  public void setCardPosition( int cardPosition )
  {
    this.cardPosition = cardPosition;
  }
  /**
   * @return the arraylist of cards in your deck
   */
  public ArrayList<Card> getDeck()
  {
    return this.deck;
  }

  /**
   * Standard constructor. If you're wondering about the necessity of the
   * context, it is used to create the database of the superclass and we need
   * it to populate the card table with the xml resource containing the starter
   * pack.
   * @param context - the context to pass to the superclass and initialize the
   * @param dbname - the name of the database to create, if null is stored in memory (according to super)
   * card table.
   */
  public Game( Context context )
  {
    super( context, GameData.DATABASE_NAME, null,
           GameData.DATABASE_VERSION );
    Log.d( TAG, "Game(Context)" );
    this.curContext = context;
    this.clearDeck();
  }
  
  /**
   * Fancy constructor designed specifically for testing. Allows for the addition of a 
   * db name parameter to create a separate db.  Refer to the Game(Context) constructor
   * for more detail. 
   * 
   * @param context - the context to pass to the superclass and initialize the
   * @param dbname - the name of the database to create, if null is stored in memory (according to super)
   * card table.
   */
  public Game( Context context, String dbname )
  {
    super( context, dbname, null,
           GameData.DATABASE_VERSION );
    Log.d( TAG, "Game(Context, String)" );
    this.curContext = context;
    this.clearDeck();
  }
  
  /**
   * Empties the current deck and instantiates a new ArrayList of cards.
   */
  public void clearDeck()
  {
    this.deck = new ArrayList<Card>();
    this.cardPosition = -1;
  }
  
  /**
   * Kill all cards that came before
   */
  public void pruneDeck()
  {
    for( int i = 0; i < this.deck.size(); ++i )
    {
      if( this.deck.get( i ).getRws() != -1 )
      {
        this.deck.remove( i );
        i--; // we removed so we need to hop back 
      }
      else
      {
        this.deck.remove( i );
        break;
      }
    }
    this.cardPosition = -1;
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

    // query for ids
    SQLiteDatabase db = this.getReadableDatabase();
    String[] columns = new String[] {GameData.Cards._ID, GameData.Cards.TITLE,
                                     GameData.Cards.BAD_WORDS};
    Cursor cur = db.query( GameData.CARD_TABLE_NAME, columns, null, null,
                         null, null, "RANDOM()");

    // iterate through the cursor pushing the ids into cardIds
    if( cur.moveToFirst() )
    {
      do
      {
        int idColumn = cur.getColumnIndex( GameData.Cards._ID );
        int titleColumn = cur.getColumnIndex( GameData.Cards.TITLE );
        int badWordsColumn = cur.getColumnIndex( GameData.Cards.BAD_WORDS );

        Card card = new Card();
        card.setId( cur.getLong( idColumn ) );
        card.setTitle( cur.getString( titleColumn ) );
        card.setBadWords( cur.getString( badWordsColumn ) );
        this.deck.add( card );

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
  public int[] getRoundScores(int teamID, int gameID)
  {
    Log.d( TAG, "getRoundScores()" );
    // query for scores
    SQLiteDatabase db = this.getReadableDatabase();
    String[] columns = new String[] {GameData.TurnScores.SCORE};
    Cursor cur = db.query( GameData.TURN_SCORES_TABLE_NAME, columns,
    		               GameData.TurnScores.TEAM_ID + "=" + teamID + " AND " +
    		               GameData.TurnScores.GAME_ID + "=" + gameID, null, null, null,
    		               GameData.TurnScores.ROUND);

    int[] scores = new int[cur.getCount()];

    // iterate through the cursor populating array of scores
    if( cur.moveToFirst() )
    {
      int i = 0;
      do
      {
        int scoreColumn = cur.getColumnIndex( GameData.TurnScores.SCORE );
        scores[i] = cur.getInt( scoreColumn );
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
    if( this.cardPosition >= this.deck.size()-1 || this.cardPosition == -1 )
    {
      this.prepDeck();
    }

    // return the card (it could be blank)
    return this.deck.get( ++this.cardPosition );
  }

  /**
   * Return the previous card
   * @return the previous card in the deck
   */
  public Card getPreviousCard()
  {
    Log.d( TAG, "getPreviousCard()" );
    
    if( this.cardPosition == 0 )
    {
      this.cardPosition = 1; 
    }
    return this.deck.get( --this.cardPosition );
  }

  /**
   * Create a game identified by the current date and return its database id
   * @return the id of the newly created game
   */
  public int newGame()
  {
    Log.d( TAG, "newGame()" );
    // Prepare the current date for insertion
    Date currentTime = new Date();
    String dateString = currentTime.toString();
    ContentValues values = new ContentValues();
    values.put( GameData.Games.TIME, dateString );
    SQLiteDatabase db = this.getWritableDatabase();

    // Do the insert and return the row id
    return (int) db.insert(GameData.GAME_TABLE_NAME, "", values);
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
		  					long turnScoreId, long rws, long time )
  {
    Log.d( TAG, "completeCard()" );
    ContentValues values = new ContentValues();
    values.put(  GameData.GameHistory.GAME_ID, gameId );
    values.put( GameData.GameHistory.TEAM_ID, teamId );
    values.put( GameData.GameHistory.CARD_ID, cardId );
    values.put( GameData.GameHistory.TURN_SCORE_ID, turnScoreId );
    values.put( GameData.GameHistory.RWS, rws );
    values.put( GameData.GameHistory.TIME, time );
    SQLiteDatabase db = this.getWritableDatabase();
    db.insert( GameData.GAME_HISTORY_TABLE_NAME, "", values );
  }
  
  /* (non-Javadoc)
   * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
   */
  @Override
  public void onCreate( SQLiteDatabase db )
  {
    Log.d( TAG, "onCreate( " + db.getPath() + ")" );

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
                GameData.GameHistory.RWS + " INTEGER," +
                GameData.GameHistory.TIME + " INTEGER);" );
    db.execSQL( "CREATE TABLE " + GameData.CARD_TABLE_NAME + " (" +
                GameData.Cards._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                GameData.Cards.PACK_NAME + " TEXT," +
                GameData.Cards.TITLE + " TEXT," +
                GameData.Cards.BAD_WORDS + " TEXT," +
                GameData.Cards.CATEGORIES + " TEXT);" );

    Log.d( TAG, "Create Table statements complete." );
    
    ContentValues values = new ContentValues();
    values.put( GameData.Teams._ID, Team.TEAMA.ordinal() );
    values.put( GameData.Teams.NAME, Team.TEAMA.name() );
    db.insert( GameData.TEAM_TABLE_NAME, "", values );
    
    values = new ContentValues();
    values.put( GameData.Teams._ID, Team.TEAMB.ordinal() );
    values.put( GameData.Teams.NAME, Team.TEAMB.name() );
    db.insert( GameData.TEAM_TABLE_NAME, "", values );
    
    values = new ContentValues();
    values.put( GameData.Teams._ID, Team.TEAMC.ordinal() );
    values.put( GameData.Teams.NAME, Team.TEAMC.name() );
    db.insert( GameData.TEAM_TABLE_NAME, "", values );
    
    values = new ContentValues();
    values.put( GameData.Teams._ID, Team.TEAMD.ordinal() );
    values.put( GameData.Teams.NAME, Team.TEAMD.name() );
    db.insert( GameData.TEAM_TABLE_NAME, "", values );
    
    InputStream starterXML =
      curContext.getResources().openRawResource(R.raw.starter);
    DocumentBuilderFactory docBuilderFactory =
      DocumentBuilderFactory.newInstance();
    
    Log.d( TAG, "Building DocBuilderFactory for card pack parsing from " + R.class.toString() );
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
    db.execSQL( "DROP TABLE IF EXISTS " + GameData.TEAM_TABLE_NAME + ";" );
    db.execSQL( "DROP TABLE IF EXISTS " + GameData.GAME_TABLE_NAME + ";" );
    db.execSQL( "DROP TABLE IF EXISTS " + GameData.TURN_SCORES_TABLE_NAME + ";" );
    db.execSQL( "DROP TABLE IF EXISTS " + GameData.FINAL_SCORES_TABLE_NAME + ";" );
    db.execSQL( "DROP TABLE IF EXISTS " + GameData.GAME_HISTORY_TABLE_NAME + ";" );
    db.execSQL( "DROP TABLE IF EXISTS " + GameData.CARD_TABLE_NAME + ";" );
    onCreate( db );
  }

  /** 
   * Overriding this function for debugging purposes.  I've added a log statement inside this
   * to track when the Game db is opened.
   * (non-Javadoc)
   * @see android.database.sqlite.SQLiteOpenHelper#onOpen(android.database.sqlite.SQLiteDatabase)
   */
  @Override
  public void onOpen(SQLiteDatabase db) {
    Log.d( TAG, "onOpen( " + db.getPath() + ")" );
    super.onOpen(db);
  }
  
}