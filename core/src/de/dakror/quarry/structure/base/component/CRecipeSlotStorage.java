/*******************************************************************************
 * Copyright 2018 Maximilian Stark | Dakror <mail@dakror.de>
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

package de.dakror.quarry.structure.base.component;

import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.RecipeList.Recipe;

/**
 * @author Maximilian Stark | Dakror
 */
public class CRecipeSlotStorage extends CInventory {
    int index;

    RecipeList recipes;

    public CRecipeSlotStorage(RecipeList recipes, int index) {
        super(Integer.MAX_VALUE);

        this.recipes = recipes;
        this.index = index;
    }

    @Override
    public boolean canAccept(ItemType item) {
        for (Recipe r : recipes.recipes) {
            if (r.getInput() == null || r.getInput().entries.length <= index) continue;

            Amount a = r.getInput().entries[index];
            // 提高缓存容量到配方量的10倍，支持批处理
            int maxCacheAmount = a.getAmount() * 10;
            if ((a.getCat() != null && item.categories.contains(a.getCat()) && getSum(a.getCat()) < maxCacheAmount)
                    || (a.getItem() != null && (a.getItem() == item || a.getItem() == Item.base(item)) && get(item) < maxCacheAmount)) return true;
        }

        return false;

    }

    @Override
    public boolean add(ItemType item, int amount) {
        for (Recipe r : recipes.recipes) {
            if (r.getInput() == null || r.getInput().entries.length <= index) continue;

            Amount a = r.getInput().entries[index];
            // 提高缓存容量到配方量的10倍，支持批处理
            int maxCacheAmount = a.getAmount() * 10;
            if ((a.getCat() != null && item.categories.contains(a.getCat()) && getSum(a.getCat()) + amount <= maxCacheAmount)
                    || (a.getItem() != null && (a.getItem() == item || a.getItem() == Item.base(item) && get(item) + amount <= maxCacheAmount))) {
                addUnsafe(item, amount);
                return true;
            }
        }

        return false;
    }

    @Override
    public int addWithRest(ItemType item, int amount) {
        throw new RuntimeException("addWithRest not supported");
    }
}
