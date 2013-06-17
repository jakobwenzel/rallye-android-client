package de.stadtrallye.rallyesoft.net;

import java.nio.charset.Charset;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.IModel;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;

@TargetApi(14)
public class NfcCallback implements CreateNdefMessageCallback {
	
	private IModel model;

	public NfcCallback(IModel model) {
		this.model = model;
	}

	@Override
	@TargetApi(14)
	public NdefMessage createNdefMessage(NfcEvent event) {
		String text = model.getLogin().toJSON();
        NdefMessage msg = new NdefMessage(
                new NdefRecord[] {
                		new NdefRecord(
                				NdefRecord.TNF_MIME_MEDIA, Std.APP_MIME.getBytes(Charset.forName("US-ASCII")),
                				new byte[0],
                				text.getBytes(Charset.forName("US_ASCII"))),
                		NdefRecord.createApplicationRecord("de.stadtrallye.rallyesoft")
        });
        return msg;
	}

}
