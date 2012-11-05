package de.stadtrallye.rallyesoft.error;

public class RestNotAvailableException extends Exception {

	public RestNotAvailableException(String rest, Exception e) {
		super("The REST Command at "+ rest +"was not available", e);
	}
}
