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

package de.stadtrallye.rallyesoft.net.retrofit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.Authenticator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.stadtrallye.rallyesoft.net.AuthManager;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * Created by Ramon on 19.09.2014.
 */
public class RetroFactory {

	private final AuthManager authManager;
	private final JacksonConverter jacksonConverter;
	private final ExecutorService executor;
	private final PreemptiveAuthentication preemptiveAuthenticator;

	public RetroFactory(AuthManager authManager) {
		this.authManager = authManager;
		this.jacksonConverter = new JacksonConverter();
		this.executor = Executors.newCachedThreadPool();
		this.preemptiveAuthenticator = new PreemptiveAuthentication(authManager);

		configureFallbackAuthentication();
	}

	private void configureFallbackAuthentication() {
		Authenticator.setDefault(authManager.getAuthenticator());
	}

	public ServerHandle getServer(String server) {
		RestAdapter restAdapter = new RestAdapter.Builder()
				.setEndpoint(server)
				.setConverter(jacksonConverter)
				.setExecutors(executor, null)
				.setRequestInterceptor(preemptiveAuthenticator)
				.build();

		return new ServerHandle(restAdapter);
	}

	public class ServerHandle {

		private final RestAdapter restAdapter;

		public ServerHandle(RestAdapter restAdapter) {
			this.restAdapter = restAdapter;
		}


		public RetroCommunicator getPublicApi() {
			return restAdapter.create(RetroCommunicator.class);
		}

		public RetroAuthCommunicator getAuthApi() throws IllegalAccessException {
			if (authManager.hasUserAuth())
				return restAdapter.create(RetroAuthCommunicator.class);
			else
				throw new IllegalAccessException("Trying to access the Auth API of a Server without having Auth data");
		}
	}
}

class PreemptiveAuthentication implements RequestInterceptor {

	private static final String AUTHORIZATION = "Authorization";
	private final AuthManager authManager;

	public PreemptiveAuthentication(AuthManager authManager) {
		this.authManager = authManager;
	}

	@Override
	public void intercept(RequestFacade request) {
		request.addHeader(AUTHORIZATION, authManager.getAuthString());
	}
}

class JacksonConverter implements Converter {
	private static final String MIME_TYPE = "application/json";

	private final ObjectMapper objectMapper;

	public JacksonConverter() {
		this(new ObjectMapper());
	}

	public JacksonConverter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override public Object fromBody(TypedInput body, Type type) throws ConversionException {
		try {
			JavaType javaType = objectMapper.getTypeFactory().constructType(type);
			return objectMapper.readValue(body.in(), javaType);
		} catch (JsonParseException e) {
			throw new ConversionException(e);
		} catch (JsonMappingException e) {
			throw new ConversionException(e);
		} catch (IOException e) {
			throw new ConversionException(e);
		}
	}

	@Override public TypedOutput toBody(Object object) {
		return new TypedJacksonOutput(object);
	}

	private class TypedJacksonOutput implements TypedOutput {


		private final Object object;

		private TypedJacksonOutput(Object object) {
			this.object = object;
		}

		@Override
		public String fileName() {
			return null;
		}

		@Override
		public String mimeType() {
			return MIME_TYPE;
		}

		@Override
		public long length() {
			return -1;
		}

		@Override
		public void writeTo(OutputStream out) throws IOException {
			objectMapper.writeValue(out, object);
		}
	}
}
