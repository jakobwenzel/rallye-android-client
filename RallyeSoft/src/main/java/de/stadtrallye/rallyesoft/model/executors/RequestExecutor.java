/*
 * Copyright (c) 2014 Jakob Wenzel, Ramon Wirsch.
 *
 * This file is part of RallySoft.
 *
 * RallyeSoft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.stadtrallye.rallyesoft.model.executors;

import de.stadtrallye.rallyesoft.net.manual.Request;
import de.stadtrallye.rallyesoft.util.IConverter;

public class RequestExecutor<T, ID> extends MyRunnable<T> {
		
	private final Request r;
	private int code;
	private final IConverter<String, T> converter;
	private final Callback<ID> callback;
	private final ID callbackId;
	
	public RequestExecutor(Request req, IConverter<String, T> converter, Callback<ID> callback, ID callbackId) {
		this.r = req;
		this.converter = converter;
		this.callback = callback;
		this.callbackId = callbackId;
	}
	
	public interface Callback<ID> {
		void executorResult(RequestExecutor<?, ID> r, ID callbackId);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T tryRun() throws Exception {
		String s = r.execute();
		code = r.getResponseCode();
		
		if (converter != null)
			return converter.convert(s);
		else
			return (T) s;
	}
	
	public int getResponseCode() {
		return code;
	}

	@Override
	protected void callback() {
		callback.executorResult(this, callbackId);
	}

}
