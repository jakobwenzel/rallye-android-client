package de.stadtrallye.rallyesoft;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import org.json.JSONException;
import org.json.JSONObject;

import de.rallye.model.structures.Chatroom;
import de.rallye.model.structures.PushEntity;
import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IChatroom;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.model.structures.ChatEntry;
import de.stadtrallye.rallyesoft.net.PushInit;

public class GCMIntentService extends GCMBaseIntentService {
	
	private static final String THIS = GCMIntentService.class.getSimpleName();

	private NotificationManager notes;

	public GCMIntentService()
	{
		super(PushInit.gcm);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		notes = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		
		NotificationCompat.BigTextStyle big = new NotificationCompat.BigTextStyle(
			new NotificationCompat.Builder(this)
				.setSmallIcon(android.R.drawable.stat_notify_chat)
				.setContentTitle("New GCM Message")
				.setContentText(extras.toString())
				.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0)))
			.bigText(extras.toString());
		
		notes.notify(":GCM Mesage", Std.GCM_NOTIFICATION, big.build());
		
		Log.i("GCMIntentService.Push", "Note: " +extras);

		try {
			PushEntity.Type type = PushEntity.Type.valueOf(extras.getString(PushEntity.TYPE));
			JSONObject payload = new JSONObject(extras.getString(PushEntity.PAYLOAD));

			switch (type) {
				case newMessage:
					ChatEntry chat = new ChatEntry.ChatConverter().doConvert(payload);
					int roomID = payload.getInt(Chatroom.CHATROOM_ID);
					Model.getModel(context).getChatroom(roomID).addChat(chat);
					break;
				case messageChanged:
					chat = new ChatEntry.ChatConverter().doConvert(payload);
					roomID = payload.getInt(Chatroom.CHATROOM_ID);
					Model.getModel(context).getChatroom(roomID).editChat(chat);
					break;
				default:
			}
		} catch (JSONException e) {
			Log.e(THIS, "Push Message not compatible", e);
		}
	}

	@Override
	protected void onError(Context context, String errorId) {
		Log.e("GCMIntentService", errorId);
	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		Log.i("GCMIntentService", "Registered GCM!");
		
		GCMRegistrar.setRegisteredOnServer(getApplicationContext(), false);
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		Log.w("GCMIntentService", "Unregistered GCM!");
//		GCMRegistrar.setRegisteredOnServer(getApplicationContext(), false);
		
		Model.getModel(getApplicationContext()).logout();
	}

}
