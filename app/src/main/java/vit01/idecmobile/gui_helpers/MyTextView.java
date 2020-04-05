package vit01.idecmobile.gui_helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatTextView;

public class MyTextView extends AppCompatTextView {
    public MyTextView(Context context) {
        super(context);
    }

    public MyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // FIXME simple workaround to https://code.google.com/p/android/issues/detail?id=191430
        // based on stackoverflow answer
        int startSelection = getSelectionStart();
        int endSelection = getSelectionEnd();
        if (startSelection != endSelection) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                final CharSequence text = getText();
                setText(null);
                setText(text);
            }
        }
        return super.dispatchTouchEvent(event);
    }
}