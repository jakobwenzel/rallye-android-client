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

public abstract class MyRunnable<T> implements Runnable {
	
	private Exception exception;
	protected T res;

	@Override
	public void run() {
		try {
			res = tryRun();
		} catch (Exception e){
			exception = e;
		} finally {
			callback();
		}
	}
	
	public boolean isSuccessful() {
		return exception == null;
	}
	
	public T getResult() {
		return res;
	}

	protected abstract T tryRun() throws Exception;
	
	protected abstract void callback();

	public Exception getException() {
		return exception;
	}
}
