/*****************************************************************************
 *  Buzzwords is a family friendly word game for mobile phones.
 *  Copyright (C) 2011 Siramix Team
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.buzzwords;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * Custom view that represents a team selection.  It can have a reference to
 * a Team object, and it displays the Team information neatly.  It also
 * handles OnClick events on its various elements, and passes them to the 
 * appropriate listeners.
 * 
 * @author The Buzzwords Team
 *
 */
public class TeamSelectLayout extends RelativeLayout {

  private Context mContext;
  
  /*
   * Elements contained in this Layout
   */
  private LinearLayout mButtons;
  private FrameLayout mFrame;
  private TextView mTeamText;
  private View mButtonAddTeam;
  private ImageButton mButtonEditTeamName;
  
  // Track if this team select is active or inactive
  private boolean mIsTeamActive;
  
  private Team mTeam;
  
  private OnTeamAddedListener mTeamAddedListener;
  private OnTeamEditedListener mTeamEditedListener;
  
  private static String TAG = "TeamSelectLayout";
  
  /**
   * @param context
   */
  public TeamSelectLayout(Context context) {
    super(context);
    mContext = context;
  }

  /**
   * @param context
   * @param attrs
   */
  public TeamSelectLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    mContext = context;
  }

  /**
   * @param context
   * @param attrs
   * @param defStyle
   */
  public TeamSelectLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mContext = context;
  }

  @Override
  public void onFinishInflate()
  {
    super.onFinishInflate();
    
    this.setFocusable(true);
    
    // Store off density in order to convert to pixels
    final float DENSITY = this.getResources().getDisplayMetrics().density;
    // Create the views

    // Initialize the group for the frame
    mFrame = new FrameLayout(mContext);
    mFrame.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    int padding = (int) (DENSITY * 1 + 0.5f);
    mFrame.setPadding(0, padding, 0, padding);
    mFrame.setBackgroundColor(R.color.black);
    
    // Initialize EditText foreground in frame
    mTeamText = new TextView(mContext);
    mTeamText.setText("Stub");
    mTeamText.setBackgroundColor(this.getResources().getColor(R.color.genericBG));
    mTeamText.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    mTeamText.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
    mTeamText.setIncludeFontPadding(false);
    mTeamText.setPadding( (int)(DENSITY * 15 + 0.5f), 0, 0, 0);
    mTeamText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 32);

    // Add the views to frame
    mFrame.addView(mTeamText);
    
    // Initialize the group for the buttons
    mButtons = new LinearLayout(mContext);
    mButtons.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    mButtons.setOrientation(LinearLayout.HORIZONTAL);
    
    // Initialize Add / Remove team button
    mButtonAddTeam = new View(mContext);
    mButtonAddTeam.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1f));
    mButtonAddTeam.setOnClickListener(mAddTeamListener);

    // Initialize EditTeamName button
    mButtonEditTeamName = new ImageButton(mContext);
    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    buttonParams.gravity = (Gravity.CENTER_VERTICAL);
    buttonParams.rightMargin = (int)(DENSITY * 10 + 0.5f);
    mButtonEditTeamName.setLayoutParams(buttonParams);
    mButtonEditTeamName.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.gamesetup_editicon_selector));
    mButtonEditTeamName.setOnClickListener(mEditTeamListener);
    
    
    // Add Buttons to button layout
    mButtons.addView(mButtonAddTeam);
    mButtons.addView(mButtonEditTeamName);
    
    
    // Add groups to TeamSelectLayout
    this.addView(mFrame);
    this.addView(mButtons);
  }

  /**
   * Set the team this TeamSelectLayout is associated with
 * @param team The team this Layout represents
 */
public void assignTeam(Team team)
  {
	  mTeam = team;
	  mTeamText.setText(team.getName());
  }

/*
 * Assign a listener to receive the OnTeamAdded callback
 * @param listener
 * 			Listener to receive the callback
 */
public void setOnTeamAddedListener(OnTeamAddedListener listener)
{
	mTeamAddedListener = listener;
}

/*
 * Assign a listener to receive the OnTeamEdited callback
 * @param listener
 * 			Listener to receive the callback
 */
public void setOnTeamEditedListener(OnTeamEditedListener listener)
{
	mTeamEditedListener = listener;
}

/*
 * Get the team assigned to this TeamSelectLayout
 */
public Team getTeam()
{
	  return mTeam;
}

/*
 * Set the view to display as active or inactive (bright or dim, for example)
 */
public void setTeamLayoutActiveness(boolean active)
{
	mIsTeamActive = active;
	if( mIsTeamActive)
	{
	    mTeamText.setBackgroundResource(mTeam.getPrimaryColor());
	    mTeamText.setTextColor(this.getResources().getColor(mTeam.getSecondaryColor()));

	    // Enable edit team name button
	    mButtonEditTeamName.setVisibility(View.VISIBLE);
	}
	else
	{
	    mTeamText.setBackgroundResource(R.color.inactiveButton);
	    mTeamText.setTextColor(this.getResources().getColor(R.color.genericBG));
	
	    // Disable edit team name button
	    mButtonEditTeamName.setVisibility(View.GONE);
	}
}

/**
 * Watches the button that edits team name
 */
private final OnClickListener mEditTeamListener = new OnClickListener() {
  public void onClick(View v) {
    if (BuzzWordsApplication.DEBUG) {
      Log.d(TAG, "AddTeamListener onClick()");
    }
    // Send event to any listeners
    if (mTeamEditedListener != null)
    {
    	mTeamEditedListener.onTeamEdited(mTeam);
    }
  }
};

  /**
   * Watches the button that adds the first team to the list
   */
  private final OnClickListener mAddTeamListener = new OnClickListener() {
    public void onClick(View v) {
      if (BuzzWordsApplication.DEBUG) {
        Log.d(TAG, "AddTeamListener onClick()");
      }
      // Toggle the view's display status
      setTeamLayoutActiveness(!mIsTeamActive);
      
      // Send event to any listeners
      if (mTeamAddedListener != null)
      {
    	  mTeamAddedListener.onTeamAdded(mTeam, mIsTeamActive);
      }
      
    }
  };
}
