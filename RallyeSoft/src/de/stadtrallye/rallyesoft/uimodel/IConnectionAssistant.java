package de.stadtrallye.rallyesoft.uimodel;

import java.net.MalformedURLException;

import de.stadtrallye.rallyesoft.model.structures.ServerLogin;

/**
 * Created by Ramon on 19.06.13.
 */
public interface IConnectionAssistant extends IProgressUI, IModelActivity {

	void next();
	void setServer(String server) throws MalformedURLException;
	String getServer();
	void setGroup(int id);

	void back();

	void setNameAndPass(String name, String pass);

	void finish(boolean acceptNewConnection);

	void login();

	int getGroup();

	String getPass();
}
