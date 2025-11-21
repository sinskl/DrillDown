/*******************************************************************************
 * Copyright 2017 Maximilian Stark | Dakror <mail@dakror.de>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.dakror.quarry.game;

import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.io.NBT.ByteArrayTag;
import de.dakror.common.libgdx.io.NBT.ByteTag;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.FloatTag;
import de.dakror.common.libgdx.io.NBT.IntTag;
import de.dakror.common.libgdx.io.NBT.ListTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.io.NBT.Tag;
import de.dakror.common.libgdx.io.NBT.TagType;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;

/**
 * @author Maximilian Stark | Dakror
 */
public class LoadingCompat {
    public static final LoadingCompat instance = new LoadingCompat();

    private LoadingCompat() {}

    public void upgrade(CompoundTag tag, int build) {
        for (int i = build + 1; i <= Quarry.Q.versionNumber; i++) {
            try {
                getClass().getMethod("version_" + i, CompoundTag.class).invoke(this, tag);
            } catch (NoSuchMethodException e) {
                continue;
            } catch (Exception e) {
                Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
            }
        }
    }

    public void version_2(CompoundTag data) throws NBTException {
        // Items moved from layer data to conveyor data
        for (Tag t : data.List("Map", TagType.Compound).data) {
            ListTag chunks = ((CompoundTag) t).List("Chunks", TagType.Compound);
            for (Tag t1 : ((CompoundTag) t).List("Items", TagType.Compound).data) {
                CompoundTag i = (CompoundTag) t1;
                for (Tag t2 : chunks.data) {
                    for (Tag t3 : ((CompoundTag) t2).List("Structures", TagType.Compound).data) {
                        CompoundTag str = (CompoundTag) t3;

                        if (str.Byte("type") == StructureType.Conveyor.id
                                || str.Byte("type") == StructureType.ConveyorBridge.id
                                || str.Byte("type") == StructureType.Hopper.id) {

                            if (!str.has("Items")) {
                                str.add(new ListTag("Items", TagType.Compound));
                            }

                            if (str.Int("x") == i.Int("x") && str.Int("y") == i.Int("y")) {
                                str.List("Items", TagType.Compound).add(i);
                            }
                        }
                    }
                }
            }
        }
    }

    public void version_16(CompoundTag data) throws NBTException {
        // procuder structures have additional byte array for input categories
        for (Tag t : data.List("Map", TagType.Compound).data) {
            ListTag chunks = ((CompoundTag) t).List("Chunks", TagType.Compound);
            for (Tag t1 : chunks.data) {
                ListTag structs = ((CompoundTag) t1).List("Structures", TagType.Compound);
                for (Tag t2 : structs.data) {
                    CompoundTag struct = (CompoundTag) t2;
                    // add empty activeCats
                    if (struct.has("activeTypes")) {
                        struct.add(new ByteArrayTag("activeCats", new byte[struct.ShortArray("activeTypes").length]));
                    }
                }
            }
        }
    }

    public void version_20(CompoundTag data) throws NBTException {
        // refinery, producerstructure, powernode all switched from int power to float power
        for (Tag tag : data.query("#Structures int,#power")) {
            tag.parent.remove(tag);
            tag.parent.add(new FloatTag("power", ((IntTag) tag).data));

        }
    }

    public void version_25(CompoundTag data) throws NBTException {
        for (Tag tag : data.query("#Structures>compound")) {
            CompoundTag t = (CompoundTag) tag;
            if (Structure.types[t.Byte("type") & 0xff] == StructureType.CableShaft) {
                t.parent.remove(t);
            }
        }
    }

    public void version_109(CompoundTag data) throws NBTException {
        // enable all outputs to ensure they are re-validated
        for (Tag tag : data.query("#Structures byte,#output")) {
            ((ByteTag) tag).data = 1;
        }
    }

    public void version_123(CompoundTag data) throws NBTException {
        // Fix StructureType ID changes after adding OreProcessingPlant (ID 74)
        // This upgrades old saves that used the original ID assignments
        
        for (Tag tag : data.query("#Structures byte,#type")) {
            ByteTag typeTag = (ByteTag) tag;
            byte oldId = typeTag.data;
            
            // Map old IDs to new IDs based on the insertion of OreProcessingPlant at ID 74
            if (oldId >= 74 && oldId <= 99) {
                // All IDs from 74-99 were shifted +1 to make room for OreProcessingPlant(74)
                typeTag.data = (byte)(oldId + 1);
            } else if (oldId == 100) {
                // ID 100 (Substation) was shifted to make room for ArcWelder(210) later
                // but in the eb55b77 commit, ArcWelder was changed from 99 to 210
                // So Substation stays at 100 in this version's mapping
                typeTag.data = (byte)100;
            }
            // ArcWelder was 99, but in eb55b77 it changed to 210
            // Old saves with ArcWelder as 99 need to be mapped to 210
            else if (oldId == 99) {
                typeTag.data = (byte)210;  // Map old ArcWelder to new ID
            }
        }
        
        // Automatically unlock OreProcessing tech for old saves
        // This ensures players can immediately build OreProcessingPlant
        byte[] sciences = data.ByteArray("Sciences", new byte[0]);
        if (sciences != null && sciences.length > 0) {
            // Check if OreProcessing (ID 2) is not already unlocked
            boolean hasOreProcessing = false;
            for (byte s : sciences) {
                if ((s & 0xff) == 2) {  // OreProcessing ID is 2
                    hasOreProcessing = true;
                    break;
                }
            }
            
            // If not unlocked and player has at least Start science (ID 0),
            // add OreProcessing to the unlocked sciences list
            if (!hasOreProcessing && sciences.length > 0) {
                byte[] newSciences = new byte[sciences.length + 1];
                System.arraycopy(sciences, 0, newSciences, 0, sciences.length);
                newSciences[newSciences.length - 1] = (byte)2;  // Add OreProcessing
                
                // Remove old Sciences tag if it exists, then add the new one
                if (data.has("Sciences")) {
                    Tag oldSciencesTag = data.get("Sciences");
                    data.remove(oldSciencesTag);
                }
                data.add(new ByteArrayTag("Sciences", newSciences));
            }
        }
    }
}
