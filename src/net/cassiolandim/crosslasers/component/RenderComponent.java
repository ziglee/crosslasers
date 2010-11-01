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
import net.cassiolandim.crosslasers.DrawableObject;
import net.cassiolandim.crosslasers.GameObject;
import net.cassiolandim.crosslasers.Vector2;
import net.cassiolandim.crosslasers.system.RenderSystem;


/** 
 * Implements rendering of a drawable object for a game object.  If a drawable is set on this
 * component it will be passed to the renderer and drawn on the screen every frame.  Drawable
 * objects may be set to be "camera-relative" (meaning their screen position is relative to the
 * location of the camera focus in the scene) or not (meaning their screen position is relative to
 * the origin at the lower-left corner of the display).
 */
public class RenderComponent extends GameComponent {

	private DrawableObject mDrawable;
    private int mPriority;
    private Vector2 mPositionWorkspace;
    private Vector2 mDrawOffset;
    
    public RenderComponent() {
        super();
        setPhase(ComponentPhases.DRAW.ordinal());
        
        mPositionWorkspace = new Vector2();
        mDrawOffset = new Vector2();
        reset();
    }
    
    @Override
    public void reset() {
        mPriority = 0;
        mDrawable = null;
        mDrawOffset.zero();
    }

    public void update(float timeDelta, BaseObject parent) {
        if (mDrawable != null) {
            RenderSystem system = sSystemRegistry.renderSystem;
            if (system != null) {
                mPositionWorkspace.set(((GameObject)parent).getPosition());
                //mPositionWorkspace.add(mDrawOffset);
                
                system.scheduleForDraw(mDrawable, mPositionWorkspace, mPriority);
            }
        }
    }

    public DrawableObject getDrawable() {
        return mDrawable;
    }
    
    public void setDrawable(DrawableObject drawable) {
        mDrawable = drawable;
    }

    public void setPriority(int priority) {
        mPriority = priority;
    }
    
    public int getPriority() {
        return mPriority;
    }

    public void setDrawOffset(float x, float y) {
        mDrawOffset.set(x, y);
    }

}
