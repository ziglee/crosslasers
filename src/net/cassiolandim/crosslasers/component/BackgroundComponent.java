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
import net.cassiolandim.crosslasers.DrawableBitmap;
import net.cassiolandim.crosslasers.DrawableFactory;
import net.cassiolandim.crosslasers.Texture;


/**
 * Adjusts the scroll position of a drawable object based on the camera's focus position.
 * May be used to scroll a ScrollableBitmap or TiledWorld to match the camera.  Uses DrawableFactory
 * to allocate fire-and-forget drawable objects every frame.
 */
public class BackgroundComponent extends GameComponent {
	
    private int mWidth;
    private int mHeight;
    private RenderComponent mRenderComponent;
    private Texture mTexture;
    
    public BackgroundComponent(float speedX, float speedY, int width, int height, Texture texture) {
        super();
        reset();
        setup(speedX, speedY, width, height);
        setUseTexture(texture);
        setPhase(ComponentPhases.PRE_DRAW.ordinal());
    }
    
    public BackgroundComponent() {
        super();
        reset();
        setPhase(ComponentPhases.PRE_DRAW.ordinal());
    }

    @Override
    public void reset() {
        mWidth = 0;
        mHeight = 0;
        mRenderComponent = null;
        mTexture = null;
    }
    
    public void setup(float speedX, float speedY, int width, int height) {
        mWidth = width;
        mHeight = height;
    }
    
    public void setUseTexture(Texture texture) {
        mTexture = texture;
    }
    
    @Override
    public void update(float timeDelta, BaseObject parent) {
        final DrawableFactory drawableFactory = sSystemRegistry.drawableFactory;
        if (mRenderComponent != null && drawableFactory != null) {
            DrawableBitmap background = drawableFactory.allocateDrawableBitmap();
            background.setTexture(mTexture);
            background.setWidth(mWidth);
            background.setHeight(mHeight);
            mRenderComponent.setDrawable(background);
        }
    }

    public void setRenderComponent(RenderComponent render) {
        mRenderComponent = render;
    }
}
