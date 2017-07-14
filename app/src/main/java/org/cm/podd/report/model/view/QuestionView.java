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

package org.cm.podd.report.model.view;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.model.DataType;
import org.cm.podd.report.model.Question;
import org.cm.podd.report.util.StyleUtil;
import org.cm.podd.report.view.NumberPickerView;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by pphetra on 9/30/14 AD.
 */
public class QuestionView extends LinearLayout {

    private final Question question;
    private EditText editView = null;
    private DatePicker calendarView = null;
    private NumberPickerView numberPicker = null;

    public QuestionView(final Context context, Question q, final boolean readonly) {
        super(context);

        this.question = q;
        final String hintText = context.getString(R.string.edittext_hint);


        setOrientation(VERTICAL);

        final LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        setLayoutParams(params);
        setTag(q.getName());
        setId(q.getId());


        TextView titleView = new TextView(context);
        titleView.setText(question.getTitle());
        titleView.setLayoutParams(params);
        titleView.setTextAppearance(context, R.style.ReportTextLabel);
        titleView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));
        titleView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (! readonly) {
                    editView.requestFocus();
                    (new android.os.Handler()).postDelayed(new Runnable() {

                        public void run() {
                            editView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                            editView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
                        }
                    }, 200);

                }
            }
        });
        addView(titleView);

        if (question.getDataType() == DataType.DATE) {
            calendarView = new DatePicker(context);
            calendarView.setCalendarViewShown(true);
            calendarView.setSpinnersShown(false);
            calendarView.setLayoutParams(params);
            calendarView.setPadding(0, 0, 0, 0);

            Date value = (Date) question.getValue();
            if (value == null) {
                value = new Date();
            }

            Calendar c = Calendar.getInstance();
            c.setTime(value);
            question.setData(c.getTime());
            
            calendarView.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), new DatePicker.OnDateChangedListener() {
                @Override
                public void onDateChanged(DatePicker datePicker, int year, int month, int day) {
                    Calendar c = Calendar.getInstance();
                    c.set(year, month, day, 0, 0, 0);
                    question.setData(c.getTime());
                }
            });

            addView(calendarView);

        } else if (question.getDataType() == DataType.INTEGER) {
            numberPicker = new NumberPickerView(context);
            numberPicker.setOrientation(HORIZONTAL);
            numberPicker.setLayoutParams(params);
            numberPicker.setPadding(0, 0, 0, 0);

            int value = 0;
            if (question.getValue() != null) {
                value = Integer.parseInt(question.getValue().toString());
                numberPicker.setValue(value);
            }

            numberPicker.setValueChangeListener(new NumberPickerView.ValueChangeListener() {
                @Override
                public void changed(int value) {
                    question.setData(value);
                }
            });

            addView(numberPicker);

        } else {
            editView = new EditText(context);
            editView.setLayoutParams(params);
            editView.setPadding(0, 0, 0, 20);
            editView.setClickable(true);
            editView.setHint(hintText);
            editView.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    editView.setHint("");
                    return false;
                }

            });

            editView.setOnFocusChangeListener(new OnFocusChangeListener() {

                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        editView.setHint(hintText);
                    }
                }
            });
            editView.setTextAppearance(context, R.style.EditTextFlat);
            editView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));
            if (readonly) {
                editView.setKeyListener(null);
                editView.setEnabled(false);
            }
            int type = 0;
            if (question.getDataType() == DataType.INTEGER) {
                type = InputType.TYPE_CLASS_NUMBER;
            }
            if (question.getDataType() == DataType.DOUBLE) {
                type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
            }
            if (question.getDataType() == DataType.STRING) {
                type = InputType.TYPE_CLASS_TEXT;
            }
            editView.setInputType(type);
            Object value = question.getValue();
            if (value != null) {
                if (question.getDataType() == DataType.DOUBLE) {
                    editView.setText(String.format( "%.2f", value ));
                } else {
                    editView.setText(value.toString());
                }
            }
            addView(editView);

            if (! readonly) {
                TextWatcher watcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        question.setData(question.getDataType().parseFromString(editable.toString()));
                    }
                };
                editView.addTextChangedListener(watcher);

                editView.setOnFocusChangeListener(new OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean hasFocus) {
                        if (! hasFocus) {
                            question.setData(question.getDataType().parseFromString(editView.getText().toString()));
                        }
                    }
                });

                editView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (listener != null) {
                            return listener.onSoftKeyAction(v, actionId, event);
                        }
                        return false;
                    }
                });
            }
        }


    }

    private SoftKeyActionHandler listener;
    public void setListener(SoftKeyActionHandler listener) {
        this.listener = listener;
    }

    public interface SoftKeyActionHandler {
        public boolean onSoftKeyAction(TextView view, int actionId, KeyEvent event);
    }

    public void askForFocus() {
        if (editView != null) {
            (new Handler()).postDelayed(new Runnable() {
                public void run() {
                    editView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                    editView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                }
            }, 100);
        }
    }

}
