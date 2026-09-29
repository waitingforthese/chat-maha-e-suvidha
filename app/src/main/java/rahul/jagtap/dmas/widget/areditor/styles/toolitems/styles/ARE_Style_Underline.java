package rahul.jagtap.dmas.widget.areditor.styles.toolitems.styles;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;

import rahul.jagtap.dmas.widget.areditor.AREditText;
import rahul.jagtap.dmas.widget.areditor.spans.AreUnderlineSpan;
import rahul.jagtap.dmas.widget.areditor.styles.ARE_ABS_Style;
import rahul.jagtap.dmas.widget.areditor.styles.toolitems.IARE_ToolItem_Updater;

public class ARE_Style_Underline extends ARE_ABS_Style<AreUnderlineSpan> {

	private ImageView mUnderlineImageView;

	private boolean mUnderlineChecked;

	private AREditText mEditText;

	private IARE_ToolItem_Updater mCheckUpdater;

	/**
	 *
	 * @param italicImage
	 */
	public ARE_Style_Underline(AREditText editText, ImageView italicImage, IARE_ToolItem_Updater checkUpdater) {
	    super(editText.getContext());
		this.mEditText = editText;
		this.mUnderlineImageView = italicImage;
		this.mCheckUpdater = checkUpdater;
		setListenerForImageView(this.mUnderlineImageView);
	}

	@Override
	public EditText getEditText() {
		return this.mEditText;
	}

	/**
	 * 
	 * @param editText
	 */
	public void setEditText(AREditText editText) {
		this.mEditText = editText;
	}

	@Override
	public void setListenerForImageView(final ImageView imageView) {
		imageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mUnderlineChecked = !mUnderlineChecked;
				if (null != mCheckUpdater) {
					mCheckUpdater.onCheckStatusUpdate(mUnderlineChecked);
				}
				if (null != mEditText) {
					applyStyle(mEditText.getEditableText(), mEditText.getSelectionStart(), mEditText.getSelectionEnd());
				}
			}
		});
	}

	@Override
	public ImageView getImageView() {
		return this.mUnderlineImageView;
	}

	@Override
	public void setChecked(boolean isChecked) {
		this.mUnderlineChecked = isChecked;
	}

	@Override
	public boolean getIsChecked() {
		return this.mUnderlineChecked;
	}

	@Override
	public AreUnderlineSpan newSpan() {
		return new AreUnderlineSpan();
	}
}
