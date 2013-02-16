package de.stadtrallye.rallyesoft.model;

import java.util.List;

public interface IPictureGallery extends List<Integer> {
	
	public enum Size {
		Small{
			@Override
			public char toChar() {
				return 's';
			}},
		Large{
			@Override
			public char toChar() {
				return 'l';
			}
		};
		public abstract char toChar();
	};
	
	public int getInitialPosition();
	
	public Size getImageSize();
	public void setImageSize(Size size);
	
	public String getPictureUrl(int pos);
}
