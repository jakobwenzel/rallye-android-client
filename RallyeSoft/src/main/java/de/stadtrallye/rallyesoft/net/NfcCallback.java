/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallyeSoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RallyeSoft is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.net;

import android.annotation.TargetApi;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.Charset;

import de.stadtrallye.rallyesoft.common.Std;
import de.stadtrallye.rallyesoft.model.Server;
import de.stadtrallye.rallyesoft.util.converters.Serialization;

@TargetApi(14)
public class NfcCallback implements CreateNdefMessageCallback {

	private static final String THIS = NfcCallback.class.getSimpleName();
	private final Server server;

	public NfcCallback(Server server) {
		this.server = server;
	}

	@Override
	@TargetApi(14)
	public NdefMessage createNdefMessage(NfcEvent event) {
		ObjectMapper mapper = Serialization.getJsonInstance();
		String text = null;
		try {
			text = mapper.writeValueAsString(server.exportLogin());
		} catch (JsonProcessingException e) {
			Log.e(THIS, "Could not export Login", e);
		}
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
