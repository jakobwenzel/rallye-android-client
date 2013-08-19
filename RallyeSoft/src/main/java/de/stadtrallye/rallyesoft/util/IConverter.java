package de.stadtrallye.rallyesoft.util;

public interface IConverter<IN, OUT> {

	public OUT convert(IN input);
}
