package org.cm.podd.report.view;

import android.content.Context;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import org.cm.podd.report.R;


public class NumberPickerView extends LinearLayout {
    private final Context mContext;
    private String mTitle;
    private View mRootView;

    private int mValue = 0;

    private EditText mValueEditText;
    private final ImageButton mAddButton;
    private final ImageButton mMinusButton;

    private ValueChangeListener listener = null;

    public NumberPickerView(Context context) {
        super(context);
        mRootView = LayoutInflater.from(context).inflate(R.layout.number_picker_layout, this, true);
        mContext = context;

        mValueEditText = (EditText) mRootView.findViewById(R.id.value_textview);
        mValueEditText.setText(String.valueOf(mValue));
        mValueEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

        mValueEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (listener != null) {
                    listener.changed(getValue());
                }
            }
        });


        mAddButton = (ImageButton) mRootView.findViewById(R.id.button_add);
        mMinusButton = (ImageButton) mRootView.findViewById(R.id.button_minus);

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
        String valueStr = mValueEditText.getText().toString();
        if (valueStr != null && !valueStr.equals("")) {
            return  Integer.parseInt(valueStr);
        }
        return 0;
    }

    public void setValue(int value) {
        mValue = value;
        mValueEditText.setText(String.valueOf(mValue));
    }


    public EditText getNumberEditText() {
        return mValueEditText;
    }

    public void clearAllListeners() {
        mAddButton.setOnClickListener(null);
        mMinusButton.setOnClickListener(null);
    }

    public void setValueChangeListener(ValueChangeListener vcl) {
        listener = vcl;
    }


    public interface ValueChangeListener {
        public void changed(int value);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {

    }
}