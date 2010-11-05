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


/**
 * TiledWorld manages a 2D map of tile indexes that define a "world" of tiles.  These may be 
 * foreground or background layers in a scrolling game, or a layer of collision tiles, or some other
 * type of tile map entirely.  The TiledWorld maps xy positions to tile indices and also handles
 * deserialization of tilemap files.
 */
public class TiledWorld extends AllocationGuard {
	
    public TiledWorld(LevelTree.Level level) {
        super();
        parseInput(level);
    }

    protected void parseInput(LevelTree.Level level) {
        
    }

}
