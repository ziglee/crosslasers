/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.cassiolandim.crosslasers.activity;

import net.cassiolandim.crosslasers.DebugLog;
import net.cassiolandim.crosslasers.EventReporter;
import net.cassiolandim.crosslasers.GLSurfaceView;
import net.cassiolandim.crosslasers.Game;
import net.cassiolandim.crosslasers.GameFlowEvent;
import net.cassiolandim.crosslasers.LevelTree;
import net.cassiolandim.crosslasers.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Debug;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

/**
 * Core activity for the game.  Sets up a surface view for OpenGL, bootstraps
 * the game engine, and manages UI events.  Also manages game progression,
 * transitioning to other activites, save game, and input events.
 */
public class AndouKun extends Activity implements SensorEventListener {
	
    private static final int ACTIVITY_CHANGE_LEVELS = 0;

    private static final int CHANGE_LEVEL_ID = Menu.FIRST;
    private static final int METHOD_TRACING_ID = CHANGE_LEVEL_ID + 1;
    
    private static final int ROLL_TO_FACE_BUTTON_DELAY = 400;
    
    public static final String PREFERENCE_LEVEL_INDEX = "levelIndex";
    public static final String PREFERENCE_LEVEL_COMPLETED = "levelsCompleted";
    public static final String PREFERENCE_SOUND_ENABLED = "enableSound";
    public static final String PREFERENCE_SAFE_MODE = "safeMode";
    public static final String PREFERENCE_SESSION_ID = "session";
    public static final String PREFERENCE_LAST_VERSION = "lastVersion";
    public static final String PREFERENCE_STATS_ENABLED = "enableStats";
    public static final String PREFERENCE_CLICK_ATTACK = "enableClickAttack";
    public static final String PREFERENCE_TILT_CONTROLS = "enableTiltControls";
    public static final String PREFERENCE_TILT_SENSITIVITY = "tiltSensitivity";
    public static final String PREFERENCE_MOVEMENT_SENSITIVITY = "movementSensitivity";
    public static final String PREFERENCE_ENABLE_DEBUG = "enableDebug";
    
    public static final String PREFERENCE_LEFT_KEY = "keyLeft";
    public static final String PREFERENCE_RIGHT_KEY = "keyRight";
    public static final String PREFERENCE_ATTACK_KEY = "keyAttack";
    public static final String PREFERENCE_JUMP_KEY = "keyJump";
    
    public static final String PREFERENCE_NAME = "CrossLasersPrefs";
    
    public static final int QUIT_GAME_DIALOG = 0;
    
    // If the version is a negative number, debug features (logging and a debug menu)
    // are enabled.
    public static final int VERSION = -1;//13;

    private GLSurfaceView mGLSurfaceView;
    private Game mGame;
    private boolean mMethodTracing;
    private int mLevelIndex;
    private SensorManager mSensorManager;
    private SharedPreferences.Editor mPrefsEditor;
    private long mLastTouchTime = 0L;
    private long mLastRollTime = 0L;
    private View mPauseMessage = null;
    
    private EventReporter mEventReporter;
    private Thread mEventReporterThread;
    
    private long mSessionId = 0L;
    
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences prefs = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
        final boolean debugLogs = prefs.getBoolean(PREFERENCE_ENABLE_DEBUG, false);
        
        if (VERSION < 0 || debugLogs) {
        	DebugLog.setDebugLogging(true);
        } else {
        	DebugLog.setDebugLogging(false);
        }
        
        DebugLog.d("AndouKun", "onCreate");
        
        
        setContentView(R.layout.main);
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.glsurfaceview);
        mPauseMessage = findViewById(R.id.pausedMessage);
        
        //mGLSurfaceView.setGLWrapper(new GLErrorLogger());
        mGLSurfaceView.setEGLConfigChooser(false); // 16 bit, no z-buffer
        //mGLSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        mGame = new Game();
        mGame.setSurfaceView(mGLSurfaceView);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int defaultWidth = 480;
        int defaultHeight = 320;
        if (dm.widthPixels != defaultWidth) {
        	float ratio =((float)dm.widthPixels) / dm.heightPixels;
        	defaultWidth = (int)(defaultHeight * ratio);
        }
        mGame.bootstrap(this, dm.widthPixels, dm.heightPixels, defaultWidth, defaultHeight);
        mGLSurfaceView.setRenderer(mGame.getRenderer());
        
        mLevelIndex = 0;
        
        
        mPrefsEditor = prefs.edit();
        mLevelIndex = prefs.getInt(PREFERENCE_LEVEL_INDEX, 0);
        int completed = prefs.getInt(PREFERENCE_LEVEL_COMPLETED, 0);
        
        // Android activity lifecycle rules make it possible for this activity to be created
        // and come to the foreground without the MainMenu Activity ever running, so in that
        // case we need to make sure that this static data is valid.
        if (!LevelTree.isLoaded()) {
        	LevelTree.loadLevelTree(R.xml.level_tree, this);
        }
        
        if (!LevelTree.levelIsValid(mLevelIndex)) {
        	// bad data?  Let's try to recover.
        	
        	if (LevelTree.levelIsValid(mLevelIndex - 1)) {
	    		// If not, try to back up a row.
	    		mLevelIndex--;
	    		completed = 0;
        	}
        }
    	
    	if (!LevelTree.levelIsValid(mLevelIndex)) {
        	// if all else fails, start the game over.
        	mLevelIndex = 0;
        	completed = 0;
    	}
        
        LevelTree.updateCompletedState(mLevelIndex, completed);
        
        mGame.setPendingLevel(LevelTree.get(mLevelIndex));
        
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // This activity uses the media stream.
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        mSessionId = prefs.getLong(PREFERENCE_SESSION_ID, System.currentTimeMillis());
        
        mEventReporter = null;
        mEventReporterThread = null;
        final boolean statsEnabled = prefs.getBoolean(PREFERENCE_STATS_ENABLED, true);
        if (statsEnabled) {
	        mEventReporter = new EventReporter();
	        mEventReporterThread = new Thread(mEventReporter);
	        mEventReporterThread.setName("EventReporter");
	        mEventReporterThread.start();
        }
    }

    
    @Override
    protected void onDestroy() {
        DebugLog.d("AndouKun", "onDestroy()");
        mGame.stop();
        if (mEventReporterThread != null) {
	        mEventReporter.stop();
	        try {
				mEventReporterThread.join();
			} catch (InterruptedException e) {
				mEventReporterThread.interrupt();
			}
        }
        super.onDestroy();
        
    }

    @Override
    protected void onPause() {
        super.onPause();
        DebugLog.d("AndouKun", "onPause");

        hidePauseMessage();
        
        mGame.onPause();
        mGLSurfaceView.onPause();
        mGame.getRenderer().onPause();	// hack!
        
        if (mMethodTracing) {
            Debug.stopMethodTracing();
            mMethodTracing = false;
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Preferences may have changed while we were paused.
        SharedPreferences prefs = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
        final boolean debugLogs = prefs.getBoolean(PREFERENCE_ENABLE_DEBUG, false);
        
        if (VERSION < 0 || debugLogs) {
        	DebugLog.setDebugLogging(true);
        } else {
        	DebugLog.setDebugLogging(false);
        }
        
        DebugLog.d("AndouKun", "onResume");
        mGLSurfaceView.onResume();
        mGame.onResume(this, false);
       
        final boolean soundEnabled = prefs.getBoolean(PREFERENCE_SOUND_ENABLED, true);
        final boolean safeMode = prefs.getBoolean(PREFERENCE_SAFE_MODE, false);
        final boolean clickAttack = prefs.getBoolean(PREFERENCE_CLICK_ATTACK, true);
        final boolean tiltControls = prefs.getBoolean(PREFERENCE_TILT_CONTROLS, false);
        final int tiltSensitivity = prefs.getInt(PREFERENCE_TILT_SENSITIVITY, 50);
        final int movementSensitivity = prefs.getInt(PREFERENCE_MOVEMENT_SENSITIVITY, 100);
        
        final int leftKey = prefs.getInt(PREFERENCE_LEFT_KEY, KeyEvent.KEYCODE_DPAD_LEFT);
        final int rightKey = prefs.getInt(PREFERENCE_RIGHT_KEY, KeyEvent.KEYCODE_DPAD_RIGHT);
        final int jumpKey = prefs.getInt(PREFERENCE_JUMP_KEY, KeyEvent.KEYCODE_SPACE);
        final int attackKey = prefs.getInt(PREFERENCE_ATTACK_KEY, KeyEvent.KEYCODE_SHIFT_LEFT);
        
        mGame.setSoundEnabled(soundEnabled);
        mGame.setControlOptions(clickAttack, tiltControls, tiltSensitivity, movementSensitivity);
        mGame.setKeyConfig(leftKey, rightKey, jumpKey, attackKey);
        mGame.setSafeMode(safeMode);
        
        if (mSensorManager != null) {
            Sensor orientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            if (orientation != null) {
                mSensorManager.registerListener(this, 
                    orientation,
                    SensorManager.SENSOR_DELAY_GAME,
                    null);
            }
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
    	if (!mGame.isPaused()) {
	        mGame.onTrackballEvent(event);
	        final long time = System.currentTimeMillis();
	        mLastRollTime = time;
    	}
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if (!mGame.isPaused()) {
    		mGame.onTouchEvent(event);
	    	
	        final long time = System.currentTimeMillis();
	        if (event.getAction() == MotionEvent.ACTION_MOVE && time - mLastTouchTime < 32) {
		        // Sleep so that the main thread doesn't get flooded with UI events.
		        try {
		            Thread.sleep(32);
		        } catch (InterruptedException e) {
		            // No big deal if this sleep is interrupted.
		        }
		        mGame.getRenderer().waitDrawingComplete();
	        }
	        mLastTouchTime = time;
    	}
        return true;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	boolean result = true;
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
			final long time = System.currentTimeMillis();
    		if (time - mLastRollTime > ROLL_TO_FACE_BUTTON_DELAY) {
    			showDialog(QUIT_GAME_DIALOG);
    			result = true;
    		}
    	} else if (keyCode == KeyEvent.KEYCODE_MENU) {
    		result = true;
    		if (mGame.isPaused()) {
    			hidePauseMessage();
    			mGame.onResume(this, true);
    		} else {
    			final long time = System.currentTimeMillis();
    	        if (time - mLastRollTime > ROLL_TO_FACE_BUTTON_DELAY) {
    	        	showPauseMessage();
    	        	mGame.onPause();
    	        }
    	        if (VERSION < 0) {
    	        	result = false;	// Allow the debug menu to come up in debug mode.
    	        }
    		}
    	} else {
		    result = mGame.onKeyDownEvent(keyCode);
		    // Sleep so that the main thread doesn't get flooded with UI events.
		    try {
		        Thread.sleep(4);
		    } catch (InterruptedException e) {
		        // No big deal if this sleep is interrupted.
		    }
    	}
        return result;
    }
     
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	boolean result = false;
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		result = true;
    	} else if (keyCode == KeyEvent.KEYCODE_MENU){ 
	        if (VERSION < 0) {
	        	result = false;	// Allow the debug menu to come up in debug mode.
	        }
    	} else {
    		result = mGame.onKeyUpEvent(keyCode);
	        // Sleep so that the main thread doesn't get flooded with UI events.
	        try {
	            Thread.sleep(4);
	        } catch (InterruptedException e) {
	            // No big deal if this sleep is interrupted.
	        }
    	}
        return result;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        boolean handled = false;
        // Only allow the debug menu in development versions.
        if (VERSION < 0) {
	        menu.add(0, CHANGE_LEVEL_ID, 0, R.string.change_level);
	        menu.add(0, METHOD_TRACING_ID, 0, R.string.method_tracing);
	        handled = true;
        }
        
        return handled;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        Intent i;
        switch(item.getItemId()) {
        case CHANGE_LEVEL_ID:
            i = new Intent(this, LevelSelectActivity.class);
            startActivityForResult(i, ACTIVITY_CHANGE_LEVELS);
            return true;
        case METHOD_TRACING_ID:
            if (mMethodTracing) {
                Debug.stopMethodTracing();
            } else {
                Debug.startMethodTracing("andou");
            }
            mMethodTracing = !mMethodTracing;
            return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        if (requestCode == ACTIVITY_CHANGE_LEVELS) {
	        if (resultCode == RESULT_OK) {
	            mLevelIndex = intent.getExtras().getInt("index");
	            saveGame();
	            
	            mGame.setPendingLevel(LevelTree.get(mLevelIndex));    
	        }  
        }
    }
    
    /*
     *  When the game thread needs to stop its own execution (to go to a new level, or restart the
     *  current level), it registers a runnable on the main thread which orders the action via this
     *  function.
     */
    public void onGameFlowEvent(int eventCode, int index) {
       switch (eventCode) {
           case GameFlowEvent.EVENT_END_GAME: 
               mGame.stop();
               finish();
               break;
           case GameFlowEvent.EVENT_RESTART_LEVEL:
    		   if (mEventReporter != null) {
        		   mEventReporter.addEvent(EventReporter.EVENT_DEATH,
        				   mGame.getLastDeathPosition().x, 
        				   mGame.getLastDeathPosition().y, 
        				   mGame.getGameTime(), 
        				   LevelTree.get(mLevelIndex).name, 
        				   VERSION, 
        				   mSessionId);
    		   }
    		   mGame.restartLevel();
    		   break;

    		   // else, fall through and go to the next level.
           case GameFlowEvent.EVENT_GO_TO_NEXT_LEVEL:
               LevelTree.get(mLevelIndex).completed = true;
               final LevelTree.Level currentLevel = LevelTree.levels.get(mLevelIndex);
               final int count = LevelTree.levels.size();
               boolean levelCompleted = true;
               
               if (mEventReporter != null) {
	               mEventReporter.addEvent(EventReporter.EVENT_BEAT_LEVEL,
	    				   0, 
	    				   0, 
	    				   mGame.getGameTime(), 
	    				   LevelTree.get(mLevelIndex).name, 
	    				   VERSION, 
	    				   mSessionId);
               }
               
               
               if (levelCompleted) {
                   mLevelIndex++;
               }
               
               if (mLevelIndex < LevelTree.levels.size()) {
        		   // go directly to the next level
                   mGame.setPendingLevel(currentLevel);
                   mGame.requestNewLevel();
            	   saveGame();
               } else {
            	   if (mEventReporter != null) {
	            	   mEventReporter.addEvent(EventReporter.EVENT_BEAT_GAME,
	        				   0, 
	        				   0, 
	        				   mGame.getGameTime(), 
	        				   "end", 
	        				   VERSION, 
	        				   mSessionId);
            	   }
                   // We beat the game!
            	   mLevelIndex = 0;
            	   saveGame();
                   mGame.stop();
                   finish();
               }
               break;
       }
    }
    
    protected void saveGame() {
    	if (mPrefsEditor != null) {
    		final int completed = LevelTree.packCompletedLevels(mLevelIndex);
    		mPrefsEditor.putInt(PREFERENCE_LEVEL_INDEX, mLevelIndex);
    		mPrefsEditor.putInt(PREFERENCE_LEVEL_COMPLETED, completed);
    		mPrefsEditor.putLong(PREFERENCE_SESSION_ID, mSessionId);
    		mPrefsEditor.commit();
    	}
    }
    
    protected void showPauseMessage() {
    	if (mPauseMessage != null) {
    		mPauseMessage.setVisibility(View.VISIBLE);
    	}
    }
    
    protected void hidePauseMessage() {
    	if (mPauseMessage != null) {
    		mPauseMessage.setVisibility(View.GONE);
    	}
    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    public void onSensorChanged(SensorEvent event) {
       synchronized (this) {
           if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
               final float x = event.values[0];
               final float y = event.values[1];
               final float z = event.values[2];
               mGame.onOrientationEvent(x, y, z);
           }
       }
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        if (id == QUIT_GAME_DIALOG) {
        	
            dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.quit_game_dialog_title)
                .setPositiveButton(R.string.quit_game_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	finish();
                    }
                })
                .setNegativeButton(R.string.quit_game_dialog_cancel, null)
                .setMessage(R.string.quit_game_dialog_message)
                .create();
        }
        return dialog;
    }
}
