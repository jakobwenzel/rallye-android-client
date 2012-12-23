package de.stadtrallye.rallyesoft.util;

public interface Converter<IN, OUT> {

	public OUT convert(IN input);

	public OUT fallback();
}
