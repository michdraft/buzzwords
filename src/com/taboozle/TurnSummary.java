package com.taboozle;

import java.util.Iterator;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
/**
 * @author The Taboozle Team
 * This activity class is responsible for summarizing the turn and the hand-off
 * into the next turn or game end.
 */
public class TurnSummary extends Activity
{
  /**
   * logging tag
   */
  public static String TAG = "TurnSummary";
  
	/**
	  * Watches the button that handles hand-off to the next turn activity.
	  */
	  private final OnClickListener NextTurnListener = new OnClickListener()
	  {
	      public void onClick(View v)
	      {
	        Log.d( TAG, "NextTurnListener OnClick()" );         
	        TaboozleApplication application =
	          (TaboozleApplication) TurnSummary.this.getApplication();
	        GameManager gm = application.GetGameManager();
	        gm.NextTurn();
     	  	startActivity(new Intent(TurnSummary.this.getApplication().getString(R.string.IntentTurn), getIntent().getData()));
	      }
	  }; // End NextTurnListener

	  /**
	   * Watches the end turn button that cancels the game.
	   */
	  private final OnClickListener EndGameListener = new OnClickListener()
	  {
	      public void onClick(View v)
	      {
          Log.d( TAG, "EndGameListener OnClick()" );         	        
	        AlertDialog confirmEnd = new AlertDialog.Builder(v.getContext()).create();
	        confirmEnd.setTitle("Confirm End Game");
	        confirmEnd.setMessage("Are you sure you want to end the current game?");
	        
	        confirmEnd.setButton("Cancel", new DialogInterface.OnClickListener() 
          {
            public void onClick(DialogInterface dialog, int which) {                           
            }
          });
	        
	        confirmEnd.setButton2("Yes", new DialogInterface.OnClickListener() 
	        {
            public void onClick(DialogInterface dialog, int which) {
              TaboozleApplication application =
                (TaboozleApplication) TurnSummary.this.getApplication();
              GameManager gm = application.GetGameManager();
              gm.EndGame();
              startActivity(new Intent(Intent.ACTION_CALL, getIntent().getData()));              
            }
          });
         
	        confirmEnd.show();
	      }
	  }; // End OnClickListener
	  
  /**
  * onCreate - initializes the activity to display the results of the turn.
  */
  @Override
  public void onCreate( Bundle savedInstanceState )
  {
  	super.onCreate( savedInstanceState );
    Log.d( TAG, "onCreate()" );  
  	// Setup the view
  	this.setContentView(R.layout.turnsummary);
  
  	ScrollView list = (ScrollView) findViewById(R.id.TurnSumCardList);
  	LinearLayout layout = new LinearLayout(this.getBaseContext());
  	layout.setOrientation(LinearLayout.VERTICAL);
      TaboozleApplication application =
          (TaboozleApplication) this.getApplication();
      GameManager game = application.GetGameManager();
  
  	LinkedList<Card> cardlist = game.GetCurrentCards();
  	Card card = null;
  	int count = 0;
  	for( Iterator<Card> it = cardlist.iterator(); it.hasNext(); )
  	{
  	  card = it.next();
  		
  	  LinearLayout line = (LinearLayout) LinearLayout.inflate(this.getBaseContext(), R.layout.turnsumrow, layout);
  	  LinearLayout realLine = (LinearLayout) line.getChildAt(count);
  	  
  	  TextView cardTitle = (TextView) realLine.getChildAt(0);
  	  cardTitle.setText(card.getTitle());
  	  
  	  ImageView cardIcon = (ImageView) realLine.getChildAt(1);
  	  cardIcon.setImageResource(card.getDrawableId());
  	  count++;
  	}
  	list.addView(layout);
  	
  	UpdateScoreViews();
  
  	Button playGameButton = (Button)this.findViewById( R.id.TurnSumNextTurn );
  	playGameButton.setOnClickListener( NextTurnListener );
  
  	Button endGameButton = (Button)this.findViewById( R.id.TurnSumEndGame );
  	endGameButton.setOnClickListener( EndGameListener );
  }
  
  /**
   * Update the views to display the proper scores for the current round
   */
  private void UpdateScoreViews()
  {
    Log.d( TAG, "UpdateScoreViews()" );  
    TaboozleApplication application =
          (TaboozleApplication) this.getApplication();
    GameManager game = application.GetGameManager();
  
  	long turnscore = game.GetTurnScore();
  	long[] totalscores = game.GetTeamScores().clone();
  
  	totalscores[game.GetActiveTeamIndex()] += turnscore;
  
  	TextView scoreview = (TextView) findViewById(R.id.TurnTotalScore);
  	scoreview.setText(Long.toString(turnscore));
  
  	int[] scoreViewIds = new int[]{R.id.TeamAScore, R.id.TeamBScore};
  	for (int i = 0; i < scoreViewIds.length; i++)
  	{
  		TextView teamTotalScoreView = (TextView) findViewById(scoreViewIds[i]);
  		teamTotalScoreView.setText(game.GetTeamNames()[i] + ": " + Long.toString(totalscores[i]));
  	}
  
      TextView curTeam = (TextView) findViewById(R.id.CurTeamIndex);
      curTeam.setText("Current Team: " + Long.toString(game.GetActiveTeamIndex()));
  }
  
  /**
   * Handler for key down events
   */
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event)
  {  
    // Handle the back button
    if( keyCode == KeyEvent.KEYCODE_BACK
        && event.getRepeatCount() == 0 )
      {
        event.startTracking();
        return true;
      }
  
    return super.onKeyDown(keyCode, event);
   }
  
  /**
   * Handler for key up events
   */
  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event)
  {
    // Make back do nothing on key-up instead of climb the action stack
    if( keyCode == KeyEvent.KEYCODE_BACK && event.isTracking()
        && !event.isCanceled() )
      {
      return true;
      }
  
    return super.onKeyUp(keyCode, event);
  }

}
