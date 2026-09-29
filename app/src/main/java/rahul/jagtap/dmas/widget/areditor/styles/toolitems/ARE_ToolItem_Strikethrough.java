package rahul.jagtap.dmas.widget.areditor.styles.toolitems;

import android.content.Context;
import android.text.Editable;
import android.text.style.CharacterStyle;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import rahul.jagtap.dmas.widget.areditor.AREditText;
import rahul.jagtap.dmas.widget.areditor.Constants;
import rahul.jagtap.dmas.R;
import rahul.jagtap.dmas.widget.areditor.Util;
import rahul.jagtap.dmas.widget.areditor.styles.IARE_Style;
import rahul.jagtap.dmas.widget.areditor.styles.toolitems.styles.ARE_Style_Strikethrough;

/**
 * Created by wliu on 13/08/2018.
 */

public class ARE_ToolItem_Strikethrough extends ARE_ToolItem_Abstract {

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
            mStyle = new ARE_Style_Strikethrough(editText, (ImageView) mToolItemView, toolItemUpdater);
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
            int size = Util.getPixelByDp(context, 40);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            imageView.setLayoutParams(params);
            imageView.setImageResource(R.drawable.strikethrough);
            imageView.bringToFront();
            mToolItemView = imageView;
        }

        return mToolItemView;
    }

    @Override
    public void onSelectionChanged(int selStart, int selEnd) {
        boolean strikethroughExists = false;

        AREditText editText = this.getEditText();
        Editable editable = editText.getEditableText();
        if (selStart > 0 && selStart == selEnd) {
            CharacterStyle[] styleSpans = editable.getSpans(selStart - 1, selStart, CharacterStyle.class);

            for (int i = 0; i < styleSpans.length; i++) {
                if (styleSpans[i] instanceof StrikethroughSpan) {
					strikethroughExists = true;
				}
            }
        } else {
			//
			// Selection is a range
			CharacterStyle[] styleSpans = editable.getSpans(selStart, selEnd, CharacterStyle.class);

			for (int i = 0; i < styleSpans.length; i++) {
				if (styleSpans[i] instanceof StrikethroughSpan) {
					if (editable.getSpanStart(styleSpans[i]) <= selStart
							&& editable.getSpanEnd(styleSpans[i]) >= selEnd) {
						strikethroughExists = true;
					}
				}
			}
		}

        mToolItemUpdater.onCheckStatusUpdate(strikethroughExists);
    }
}
