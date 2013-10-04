package de.stadtrallye.rallyesoft;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import org.json.JSONException;
import org.json.JSONObject;

import de.rallye.model.structures.Chatroom;
import de.rallye.model.structures.PushEntity;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.converters.JsonConverters;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.net.PushInit;

public class GCMIntentService extends GCMBaseIntentService {
	
	private static final String THIS = GCMIntentService.class.getSimpleName();

	public GCMIntentService()
	{
		super(PushInit.gcm);
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		Bundle extras = intent.getExtras();

		Log.i(THIS, "Received Push: " +extras);

		try {
			PushEntity.Type type = PushEntity.Type.valueOf(extras.getString(PushEntity.TYPE));
			JSONObject payload = new JSONObject(extras.getString(PushEntity.PAYLOAD));

			switch (type) {
				case newMessage:
					ChatEntry chat = new JsonConverters.ChatConverter().doConvert(payload);
					int roomID = payload.getInt(Chatroom.CHATROOM_ID);
					Model.getInstance(context).getChatroom(roomID).pushChat(chat);
					break;
				case messageChanged:
					chat = new JsonConverters.ChatConverter().doConvert(payload);
					roomID = payload.getInt(Chatroom.CHATROOM_ID);
					Model.getInstance(context).getChatroom(roomID).editChat(chat);
					break;
				default:
			}
		} catch (JSONException e) {
			Log.e(THIS, "Push Message not compatible", e);
		}
	}

	@Override
	protected void onError(Context context, String errorId) {
		Log.e(THIS, errorId);
	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		Log.i(THIS, "Registered GCM!");
		
		GCMRegistrar.setRegisteredOnServer(getApplicationContext(), false);
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		Log.w(THIS, "Unregistered GCM!");
//		GCMRegistrar.setRegisteredOnServer(getApplicationContext(), false);
		
		Model.getInstance(getApplicationContext()).logout();
	}

}