package org.cm.podd.report.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.cm.podd.report.R;


public class NumberPickerView extends LinearLayout {
    private final Context mContext;
    private String mTitle;
    private View mRootView;

    private int mValue = 0;

    private EditText mValueEditText;

    public NumberPickerView(Context context) {
        super(context);
        mRootView = LayoutInflater.from(context).inflate(R.layout.number_picker_layout, this);
        mContext = context;

        mValueEditText = (EditText) mRootView.findViewById(R.id.value_textview);
        mValueEditText.setText(String.valueOf(mValue));
        mValueEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

        ImageButton mAddButton = (ImageButton) mRootView.findViewById(R.id.button_add);
        ImageButton mMinusButton = (ImageButton) mRootView.findViewById(R.id.button_minus);

        mValueEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                mValue = Integer.parseInt(editable.toString());
            }
        });

        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mValue++;
                mValueEditText.setText(String.valueOf(mValue));
            }
        });

        mMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mValue--;
                if (mValue < 0) {
                    mValue = 0;
                }
                mValueEditText.setText(String.valueOf(mValue));
            }
        });
    }

    public int getValue() {
        return mValue;
    }

    public void setValue(int value) {
        mValue = value;
        mValueEditText.setText(String.valueOf(mValue));
    }

    public EditText getNumberEditText() {
        return mValueEditText;
    }
}