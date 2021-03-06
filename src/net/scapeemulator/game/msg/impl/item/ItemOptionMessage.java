/**
 * Copyright (c) 2012, Hadyn Richard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal 
 * in the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN 
 * THE SOFTWARE.
 */

package net.scapeemulator.game.msg.impl.item;

import net.scapeemulator.game.model.Option;
import net.scapeemulator.game.msg.Message;

/**
 * Created by Hadyn Richard
 */
public final class ItemOptionMessage extends Message {

    private final Option option;
    private final int id, slot, widgetHash;
    
    public ItemOptionMessage(int id, int slot, int widgetHash, Option option) {
        this.id = id;
        this.slot = slot;
        this.widgetHash = widgetHash;
        this.option = option;
    }
    
    public int getId() {
        return id;
    }
    
    public int getSlot() {
        return slot;
    }
    
    public int getWidgetHash() {
        return widgetHash;
    }
    
    public Option getOption() {
        return option;
    }
}
