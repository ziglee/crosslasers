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

package net.cassiolandim.crosslasers.component;

import net.cassiolandim.crosslasers.BaseObject;
import net.cassiolandim.crosslasers.GameObject;
import net.cassiolandim.crosslasers.Utils;
import net.cassiolandim.crosslasers.Vector2;
import net.cassiolandim.crosslasers.GameObject.ActionType;
import net.cassiolandim.crosslasers.system.HudSystem;

public class NPCComponent extends GameComponent {
	
    private float mPauseTime;
    private float mTargetXVelocity;
    private int mLastHitTileX;
    private int mLastHitTileY;
    
    private int[] mQueuedCommands;
    private int mQueueTop;
    private int mQueueBottom;
    private boolean mExecutingQueue;
    
    private Vector2 mPreviousPosition;
    
    private float mUpImpulse;
    private float mDownImpulse;
    private float mHorizontalImpulse;
    private float mSlowHorizontalImpulse;
    private float mAcceleration;
    
    private int mGameEvent;
    private int mGameEventIndex;
    private boolean mSpawnGameEventOnDeath;
    
    private boolean mReactToHits;
    private boolean mFlying;
    private boolean mPauseOnAttack;
    
    private float mDeathTime;
	private float mDeathFadeDelay;
    
    private static final float UP_IMPULSE = 400.0f;
    private static final float DOWN_IMPULSE = -10.0f;
    private static final float HORIZONTAL_IMPULSE = 200.0f;
    private static final float SLOW_HORIZONTAL_IMPULSE = 50.0f;
    private static final float ACCELERATION = 300.0f;
    private static final float HIT_IMPULSE = 300.0f;
    private static final float HIT_ACCELERATION = 700.0f;

    private static final float DEATH_FADE_DELAY = 4.0f;
    
    private static final float PAUSE_TIME_SHORT = 1.0f;
    private static final float PAUSE_TIME_MEDIUM = 4.0f;
    private static final float PAUSE_TIME_LONG = 8.0f;
    private static final float PAUSE_TIME_ATTACK = 1.0f;
    private static final float PAUSE_TIME_HIT_REACT = 1.0f;
    
    private static final int COMMAND_QUEUE_SIZE = 16;
    
    public NPCComponent() {
        super();
        setPhase(ComponentPhases.THINK.ordinal());
        mQueuedCommands = new int[COMMAND_QUEUE_SIZE];
        mPreviousPosition = new Vector2();
        reset();
    }
    
    @Override
    public void reset() {
        mPauseTime = 0.0f;
        mTargetXVelocity = 0.0f;
        mLastHitTileX = 0;
        mLastHitTileY = 0;
        mQueueTop = 0;
        mQueueBottom = 0;
        mPreviousPosition.zero();
        mExecutingQueue = false;
        mUpImpulse = UP_IMPULSE;
        mDownImpulse = DOWN_IMPULSE;
        mHorizontalImpulse = HORIZONTAL_IMPULSE;
        mSlowHorizontalImpulse = SLOW_HORIZONTAL_IMPULSE;
        mAcceleration = ACCELERATION;
        mGameEvent = -1;
        mGameEventIndex = -1;
        mSpawnGameEventOnDeath = false;
        mReactToHits = false;
        mFlying = false;
        mDeathTime = 0.0f;
        mDeathFadeDelay = DEATH_FADE_DELAY;
        mPauseOnAttack = true;
    }

    @Override
    public void update(float timeDelta, BaseObject parent) {

        GameObject parentObject = (GameObject)parent;
        
        if (mReactToHits && 
        		mPauseTime <= 0.0f && 
        		parentObject.getCurrentAction() == ActionType.HIT_REACT) {
        	mPauseTime = PAUSE_TIME_HIT_REACT;
            pauseMovement(parentObject);
        	parentObject.getVelocity().x = -parentObject.facingDirection.x * HIT_IMPULSE;
        	parentObject.getAcceleration().x = HIT_ACCELERATION;

        } else if (parentObject.getCurrentAction() == ActionType.DEATH) {        	
        	if (mSpawnGameEventOnDeath && mGameEvent != -1) {
        		if (Utils.close(parentObject.getVelocity().x, 0.0f) 
        				&& parentObject.touchingGround()) {

        			if (mDeathTime < mDeathFadeDelay && mDeathTime + timeDelta >= mDeathFadeDelay) {
        				HudSystem hud = sSystemRegistry.hudSystem;
        	        	
        	        	if (hud != null) {
        	        		hud.startFade(false, 1.5f);
        	        		hud.sendGameEventOnFadeComplete(mGameEvent, mGameEventIndex);
        	        		mGameEvent = -1;
        	        	}
        			}
        			mDeathTime += timeDelta;

        		}
        	}
        	// nothing else to do.
        	return;
        } else if (parentObject.life <= 0) {
        	parentObject.setCurrentAction(ActionType.DEATH);
        	parentObject.getTargetVelocity().x = 0;
        	return;
        } else if (parentObject.getCurrentAction() == ActionType.INVALID ||
        		(!mReactToHits && parentObject.getCurrentAction() == ActionType.HIT_REACT)) {
        	parentObject.setCurrentAction(ActionType.MOVE);
        } 
        
        if (mPauseTime <= 0.0f) {

        } else {
            mPauseTime -= timeDelta;
            if (mPauseTime < 0.0f) {
                resumeMovement(parentObject);
                mPauseTime = 0.0f;
                parentObject.setCurrentAction(ActionType.MOVE);
            }
        }
        
        mPreviousPosition.set(parentObject.getPosition());
    }
    
    private void pauseMovement(GameObject parentObject) {
    	mTargetXVelocity = parentObject.getTargetVelocity().x;
    	parentObject.getTargetVelocity().x = 0.0f;
    	parentObject.getVelocity().x = 0.0f;
    }
    
    private void resumeMovement(GameObject parentObject) {
    	parentObject.getTargetVelocity().x = mTargetXVelocity;
    	parentObject.getAcceleration().x = mAcceleration;
    }
    
    private void queueCommand(int hotspot) {
    	int nextSlot = (mQueueBottom + 1) % COMMAND_QUEUE_SIZE;
    	if (nextSlot != mQueueTop) { // only comply if there is space left in the buffer 
    		mQueuedCommands[mQueueBottom] = hotspot;
    		mQueueBottom = nextSlot;
    	}
    }
    
    public void setSpeeds(float horizontalImpulse, float slowHorizontalImpulse, float upImpulse, float downImpulse, float acceleration) {
    	mHorizontalImpulse = horizontalImpulse;
    	mSlowHorizontalImpulse = slowHorizontalImpulse;
    	mUpImpulse = upImpulse;
    	mDownImpulse = downImpulse;
    	mAcceleration = acceleration;
    }
    
    public void setGameEvent(int event, int index, boolean spawnOnDeath) {
    	mGameEvent = event;
    	mGameEventIndex = index;
    	mSpawnGameEventOnDeath = spawnOnDeath;
    }
    
    public void setReactToHits(boolean react) {
    	mReactToHits = react;
    }

    public void setFlying(boolean flying) {
    	mFlying = flying;
    }
    
    public void setPauseOnAttack(boolean pauseOnAttack) {
    	mPauseOnAttack = pauseOnAttack;
    }
}
