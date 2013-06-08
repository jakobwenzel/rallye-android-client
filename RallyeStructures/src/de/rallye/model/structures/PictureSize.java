package de.rallye.model.structures;

import java.awt.Dimension;

public enum PictureSize {
	Thumbnail, Standard, Original;
	
	private static final int THUMB_SIZE = 96;
	private static final int STD_SIZE = 1280;
	
	private static final String[] abr =  new String[]{ "thumb", "std", "org" };
	private static final Dimension[] dims = new Dimension[]{ new Dimension(THUMB_SIZE, THUMB_SIZE), new Dimension(STD_SIZE, STD_SIZE), null };
	
	public static PictureSize fromString(String s) {
		for(int i=0; i<abr.length; i++) {
			if (abr[i].equalsIgnoreCase(s))
				return PictureSize.values()[i];
		}
		return null;
	}
	
	public String toShortString() {
		return abr[this.ordinal()];
	}
	
	public Dimension getDimension() {
		return dims[this.ordinal()];
	}
	
	public static class PictureSizeString {
		
		final public PictureSize size;
		
		public PictureSizeString(String s) {
			PictureSize size;
			
			try {
				size = PictureSize.valueOf(s);
			} catch (Exception e) {
				size = PictureSize.fromString(s);
			}
			
			this.size = (size == null)? PictureSize.Standard : size;
		}
	}
}
