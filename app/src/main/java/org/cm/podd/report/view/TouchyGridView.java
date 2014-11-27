/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cm.podd.report.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.GridView;

public class TouchyGridView extends GridView
{
    public TouchyGridView(Context context) {
        super(context);
    }

    public TouchyGridView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public TouchyGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private OnNoItemClickListener listener;
    public interface OnNoItemClickListener
    {
        public void onNoItemClick();
    }

    public void setOnNoItemClickListener(OnNoItemClickListener listener)
    {
        this.listener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        // The pointToPosition() method returns -1 if the touch event
        // occurs outside of a child View.
        // Change the MotionEvent action as needed. Here we use ACTION_DOWN
        // as a simple, naive indication of a click.
        if (pointToPosition((int) event.getX(), (int) event.getY()) == -1
                && event.getAction() == MotionEvent.ACTION_DOWN)
        {
            if (listener != null)
            {
                listener.onNoItemClick();
            }
        }
        return super.dispatchTouchEvent(event);
    }
}