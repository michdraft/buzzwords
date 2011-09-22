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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

public class BuzzerService {

  /**
   * Instance of the BuzzerService that can be called
   */
  private static BuzzerService _instance = new BuzzerService();

  /**
   * TODO: Determine if this is the best way to check the state.
   */
  private static boolean isBuzzing = false;
  
  /**
   * Holds the unique ID for the game being connected to
   */
  private static String gameToken;

  private final static String TAG = "BuzzerService";

  private BuzzerService() {
    setToken();
  }
  
  /**
   * Get the current instance of the BuzzerService
   * @return the current instance of the BuzzerService
   */	
  public static BuzzerService getInstance() {
    return _instance;
  }

  public String getLocaltoken() {
    return gameToken;
  }
  
  public boolean getLocalisBuzzing() {
	return isBuzzing;
  }
  /**
   * Takes a response from the server and processes the data on
   * the Buzzer's device.
   * @param gameData
   */
  public void updateBuzzer(JSONObject gameData) {
	
  }
  
  /**
   * Takes a response from the server and processes the data on
   * the Presenter's device.
   * @param gameData
   */
  public void updatePresenter(JSONObject gameData) {
    if (BuzzWordsApplication.DEBUG) {
      Log.d(TAG, "updatePresenter(" + gameToken + ")");
    }		
  
  }
  
  public static boolean getBuzzing(String token) {
    if (BuzzWordsApplication.DEBUG) {
      Log.d(TAG, "getBuzz(" + token + ")");
    }		
/*
    HttpClient httpClient = new DefaultHttpClient();
    HttpPost httpPost = new HttpPost("http://www.siramix.com/buzz/server/getbuzz.php");		

    try {
      List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
      nameValuePairs.add(new BasicNameValuePair("token", token));      
      httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
      HttpResponse response = httpClient.execute(httpPost);    
      JSONObject responseJSON = responseToJSON(response);
      if (validateResponse(responseJSON) == false) {
        isBuzzing = false;
      }
      
      if (Integer.parseInt(responseJSON.getString("buzzing")) == 1) {
        isBuzzing = true;
      }

    } catch (Exception e) {
        Log.v("ioexception", e.toString());
        isBuzzing = false;
    }
*/
    return isBuzzing;	  
  }

  /**
   * Processes a call to synchronize and start a networked game.
   * @param gameToken
   */
  public void setToken() {
    if (BuzzWordsApplication.DEBUG) {
      Log.d(TAG, "syncGame(" + gameToken + ")");
    }		
    
    JSONObject responseJSON = sendBuzzServerRequest
        ("http://www.siramix.com/buzz/server/newgame.php?", null, "31.5", "31.5", null, null);
    
    try {
      BuzzerService.gameToken = responseJSON.getString("token");
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }  
  
  /**
   * Processes a call to synchronize and start a networked game.
   * @param gameToken
   */
  public boolean syncGame(String gameToken) {
    if (BuzzWordsApplication.DEBUG) {
	  Log.d(TAG, "syncGame(" + gameToken + ")");
	}		

    HttpClient httpClient = new DefaultHttpClient();
    HttpPost httpPost = new HttpPost("http://www.siramix.com/buzz/server/isgamevalid.php");		

    try {
      List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
      nameValuePairs.add(new BasicNameValuePair("token", gameToken));      
      httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
      HttpResponse response = httpClient.execute(httpPost); 
      /*if (validateResponse(response)) {
        return true;
      }*/     
    } catch (Exception e) {
      Log.v("ioexception", e.toString());
    }
    
    return false;     
  }
  
  /**
   * Sets the local service to the buzzing state and tells the 
   * server to do the same.  A successful response will be received
   * from the server if the server sets the state to 'buzzing'.
   * 
   * @param gameToken
   */
  public void setBuzzing(String gameToken) {
	if (BuzzWordsApplication.DEBUG) {
	  Log.d(TAG, "setBuzzing(" + gameToken + ")");
	}
	
	isBuzzing = true;
	
	HttpClient httpClient = new DefaultHttpClient();
	HttpPost httpPost = new HttpPost("http://www.siramix.com/buzz/server/setbuzz.php");
	
    try {
      List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
      nameValuePairs.add(new BasicNameValuePair("token", gameToken));
      httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

      HttpResponse response = httpClient.execute(httpPost);
      //validateResponse(response); // stub status check
      } catch (Exception e) {
        Log.v("ioexception", e.toString());
      }
  }

  /**
   * Sets the local service to the buzzing state and tells the 
   * server to do the same.  A successful response will be received
   * from the server if the server sets the state to 'buzzing'.
   * 
   * @param gameToken
   */
  public void unsetBuzzing(String gameToken) {
	if (BuzzWordsApplication.DEBUG) {
	  Log.d(TAG, "setBuzzing(" + gameToken + ")");
	}
	
	isBuzzing = true;
	
	HttpClient httpClient = new DefaultHttpClient();
	HttpPost httpPost = new HttpPost("http://www.siramix.com/buzz/server/unsetbuzz.php");
	
    try {
      List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
      nameValuePairs.add(new BasicNameValuePair("token", gameToken));   
      httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

      HttpResponse response = httpClient.execute(httpPost);
      //validateResponse(response); // stub status check
      } catch (Exception e) {
        Log.v("ioexception", e.toString());
      }
  }
  
  private static JSONObject sendBuzzServerRequest(String URL, 
                  String token, String lat, String lon, String cardTitle, String Badwords) {
    
    HttpClient httpClient = new DefaultHttpClient();
    HttpPost httpPost = new HttpPost(URL);
    
    try {
      List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
      if (token != null) {
        nameValuePairs.add(new BasicNameValuePair("token", token));
      }
      if (lat != null) {
        nameValuePairs.add(new BasicNameValuePair("lat", lat));
      }
      if (lon != null) {
        nameValuePairs.add(new BasicNameValuePair("lon", lon));
      }
      if (cardTitle != null) {
        nameValuePairs.add(new BasicNameValuePair("cardTitle", cardTitle));
      }
      if (Badwords != null) {
        nameValuePairs.add(new BasicNameValuePair("badWords", Badwords));
      }
      
      Log.d("********************" + TAG, nameValuePairs.toString());
      httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
      HttpResponse response = httpClient.execute(httpPost);
      BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
      String json = reader.readLine();
      JSONTokener tokener = new JSONTokener(json);
      JSONObject finalResult = new JSONObject(tokener);
      
      return finalResult;
      } catch (Exception e) {
        Log.v("ioexception", e.toString());
        return null;
      }
  }
  
  /**
   * Returns true for good response, false for bad
   * @param response
   * @return
   * @throws JSONException 
   * @throws NumberFormatException 
   */
  private static boolean validateResponse(JSONObject resultJSON) throws NumberFormatException, JSONException {
      int status = Integer.parseInt(resultJSON.getString("status"));
      if (status == 1) {
    	  return true;
      }
      else {
        return false;
      }
    }
  
}