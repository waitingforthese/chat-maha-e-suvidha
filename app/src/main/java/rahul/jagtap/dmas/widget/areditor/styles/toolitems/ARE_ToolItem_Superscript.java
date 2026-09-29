package rahul.jagtap.dmas.widget.areditor.styles.toolitems;

import android.content.Context;
import android.text.Editable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import rahul.jagtap.dmas.widget.areditor.AREditText;
import rahul.jagtap.dmas.widget.areditor.Constants;
import rahul.jagtap.dmas.R;
import rahul.jagtap.dmas.widget.areditor.Util;
import rahul.jagtap.dmas.widget.areditor.spans.AreSuperscriptSpan;
import rahul.jagtap.dmas.widget.areditor.styles.IARE_Style;
import rahul.jagtap.dmas.widget.areditor.styles.toolitems.styles.ARE_Style_Superscript;

/**
 * Created by wliu on 13/08/2018.
 */

public class ARE_ToolItem_Superscript extends ARE_ToolItem_Abstract {

    @Override
    public IARE_ToolItem_Updater getToolItemUpdater() {
        if (mToolItemUpdater == null) {
            mToolItemUpdater = new ARE_ToolItem_UpdaterDefault(this, Constants.CHECKED_COLOR, Constants.UNCHECKED_COLOR);
            setToolItemUpdater(mToolItemUpdater);
        }
        return mToolItemUpdater;
    }

    @Override
    public IARE_Style getStyle() {
        if (mStyle == null) {
            AREditText editText = this.getEditText();
            IARE_ToolItem_Updater toolItemUpdater = getToolItemUpdater();
            mStyle = new ARE_Style_Superscript(editText, (ImageView) mToolItemView, toolItemUpdater);
        }
        return mStyle;
    }

    @Override
    public View getView(Context context) {
        if (null == context) {
            return mToolItemView;
        }
        if (mToolItemView == null) {
            ImageView imageView = new ImageView(context);
            int size = Util.getPixelByDp(context, 30);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            imageView.setLayoutParams(params);
            imageView.setImageResource(R.drawable.superscript);
            imageView.bringToFront();
            mToolItemView = imageView;
        }

        return mToolItemView;
    }

    @Override
    public void onSelectionChanged(int selStart, int selEnd) {

		boolean superscriptExists = false;

		//
		// Two cases:
		// 1. Selection is just a pure cursor
		// 2. Selection is a range
		Editable editable = this.getEditText().getEditableText();
		if (selStart > 0 && selStart == selEnd) {
			AreSuperscriptSpan[] superscriptSpans = editable.getSpans(selStart - 1, selStart, AreSuperscriptSpan.class);
			if (superscriptSpans != null && superscriptSpans.length > 0) {
				superscriptExists = true;
			}
		} else {
            AreSuperscriptSpan[] superscriptSpans = editable.getSpans(selStart, selEnd, AreSuperscriptSpan.class);
            if (superscriptSpans != null && superscriptSpans.length > 0) {
                if (editable.getSpanStart(superscriptSpans[0]) <= selStart
                        && editable.getSpanEnd(superscriptSpans[0]) >= selEnd) {
                    superscriptExists = true;
                }
            }
        }

        mToolItemUpdater.onCheckStatusUpdate(superscriptExists);
    }
}
