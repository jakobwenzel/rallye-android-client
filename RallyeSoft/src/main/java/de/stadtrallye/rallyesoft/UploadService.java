package de.stadtrallye.rallyesoft;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;
import de.stadtrallye.rallyesoft.model.Model;
import de.stadtrallye.rallyesoft.net.Request;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class UploadService extends IntentService {

	private static final String THIS = UploadService.class.getSimpleName();

	private static final String NOTE_TAG = ":uploader";

	private static final int MAX_SIZE = 1000;

	private final IModel model;
	private NotificationManager notes;

	public UploadService() {
		super("RallyePictureUpload");
		
		this.model = Model.getInstance(this);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		notes = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String picture = intent.getStringExtra(Std.PIC);
		String hash = intent.getStringExtra(Std.HASH);
		String mime = intent.getStringExtra(Std.MIME);

        int size, current = 0;

        try {
			InputStream fIn = getContentResolver().openInputStream(Uri.parse(picture));

			Bitmap img = BitmapFactory.decodeStream(fIn);

			int biggerSide = (img.getWidth()>img.getHeight())?img.getWidth():img.getHeight();
			double factor = MAX_SIZE*1.0/biggerSide;

			int w = (int)Math.round(img.getWidth()*factor);
			int h = (int)Math.round(img.getHeight()*factor);
			Bitmap scaled = img.createScaledBitmap(img,w,h,true);

			ByteArrayOutputStream outTemp = new ByteArrayOutputStream();
			scaled.compress(Bitmap.CompressFormat.JPEG,80,outTemp);

			byte[] buf = outTemp.toByteArray();
			size = buf.length;

			URL url = model.getPictureUploadURL(hash);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod(Request.RequestType.PUT.toString());
			con.setRequestProperty(Std.CONTENT_TYPE, mime);
			con.setDoOutput(true);

			NotificationCompat.Builder note = new NotificationCompat.Builder(this)
					.setSmallIcon(R.drawable.ic_upload_light)
					.setContentTitle(getString(R.string.uploading) + "...")
					.setContentText(picture);

			OutputStream out = con.getOutputStream();



			final int step = 10000;
			while (current<size) {
				int currentStep = (step>size-current)?size-current:step;
				out.write(buf,current,currentStep);
				current+=currentStep;

				notes.notify(NOTE_TAG, R.id.uploader, note.setProgress((int) size / 1000, (int) current / 1000, false).build());
			}

			notes.notify(NOTE_TAG, R.id.uploader, note.setProgress(0,0,false).setContentTitle(getString(R.string.upload_complete)).build());
			out.close();
			int code = con.getResponseCode();
			if (code >= 300) {
				Log.e(THIS, code + ": " + con.getResponseMessage());
			} else
				Log.i(THIS, picture +" successfully uploaded ("+size+" bytes)");
			con.disconnect();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
