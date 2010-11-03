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

package net.cassiolandim.crosslasers;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.XmlResourceParser;

public final class LevelTree {

    public static class Level {
    	public Long index;
        public String name;
        public boolean completed;
        public ArrayList<EnemyEntry> enemyEntries = new ArrayList<EnemyEntry>();
        
        public Level(Long index, String title) {
        	this.index = index;
        	this.name = title;
        	this.completed = false;
        }
    }
    
    public static class EnemyEntry {
    	public Integer type = 0;
    	public Integer quantity = 0;
    	public Long spawnDelay = 0L;
    	public Long minInterval = 0L;
    	
    	public EnemyEntry(Integer type, Integer quantity, Long spawnDelay, Long minInterval){
    		this.type = type;
    		this.quantity = quantity;
    		this.spawnDelay = spawnDelay;
    		this.minInterval = minInterval;
    	}
    }
    
    public final static ArrayList<Level> levels = new ArrayList<Level>();
    private static boolean mLoaded = false;
    
    public static final Level get(int index) {
    	return levels.get(index);
    }
    
    public static final boolean isLoaded() {
    	return mLoaded;
    }
    
    public static final void loadLevelTree(int resource, Context context) {
        if (mLoaded) return;
        
    	XmlResourceParser parser = context.getResources().getXml(resource);
        
        levels.clear();
        
        Level currentLevel = null;
        
        try { 
            int eventType = parser.getEventType(); 
            while (eventType != XmlPullParser.END_DOCUMENT) { 
                if(eventType == XmlPullParser.START_TAG) { 
                    if (parser.getName().equals("level")) { 
                    	String titleString = null;
                    	Long index = null;
                        for(int i = 0; i < parser.getAttributeCount(); i++) { 
                    		if (parser.getAttributeName(i).equals("index")) {
                				index = Long.getLong(parser.getAttributeValue(i));
                    		}
                    		if (parser.getAttributeName(i).equals("title")) {
                				titleString = parser.getAttributeValue(i);
                    		}
                        } 
                        currentLevel = new Level(index, titleString);
                        levels.add(currentLevel);
                    }
                    
                    if (parser.getName().equals("enemy") && currentLevel != null) {
                    	Integer type = null;
                    	Integer quantity = null;
                    	Long spawnDelay = null;
                    	Long minInterval = null;
                    	for(int i = 0; i < parser.getAttributeCount(); i++) { 
                    		if (parser.getAttributeName(i).equals("type")) {
                    			type = Integer.getInteger(parser.getAttributeValue(i));
                    		}
                    		if (parser.getAttributeName(i).equals("quantity")) {
                    			quantity = Integer.getInteger(parser.getAttributeValue(i));
                    		}
                    		if (parser.getAttributeName(i).equals("spawnDelay")) {
                    			spawnDelay = Long.getLong(parser.getAttributeValue(i));
                    		}
                    		if (parser.getAttributeName(i).equals("minInterval")) {
                    			minInterval = Long.getLong(parser.getAttributeValue(i));
                    		}
                        }
                    	EnemyEntry enemyEntry = new EnemyEntry(type, quantity, spawnDelay, minInterval);
                    	currentLevel.enemyEntries.add(enemyEntry);
                    }
                } 
                eventType = parser.next(); 
            } 
        } catch(Exception e) { 
                DebugLog.e("LevelTree", e.getStackTrace().toString()); 
        } finally { 
            parser.close(); 
        } 
        mLoaded = true;
    }
    
	public final static void updateCompletedState(int levelIndex, int completedLevels) {
		final int indexCount = levels.size();
		for (int x = 0; x < indexCount; x++) {
			final Level level = levels.get(x);
			if (x < levelIndex) {
				level.completed = true;
			} else if (x == levelIndex) {
				if ((completedLevels & (1 << x)) != 0) {
					level.completed = true;
				}
			} else {
				level.completed = false;
			}
		}
	}

	public final static int packCompletedLevels(int levelIndex) {
		int completed = 0;
		final Level level = levels.get(levelIndex);
		if (level.completed) {
			completed |= 1 << levelIndex;
		}
		return completed;
	}

	public static boolean levelIsValid(int index) {
		return (index >= 0 && index < levels.size());
	}
}
