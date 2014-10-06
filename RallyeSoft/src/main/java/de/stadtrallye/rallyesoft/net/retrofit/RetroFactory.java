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
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RallyeSoft. If not, see <http://www.gnu.org/licenses/>.
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

import de.stadtrallye.rallyesoft.net.AuthProvider;
import de.stadtrallye.rallyesoft.threading.Threading;
import de.stadtrallye.rallyesoft.util.converters.Serialization;
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

	private final JacksonConverter converter;
	private final ExecutorService executor;

	public RetroFactory() {
		this.converter = new JacksonConverter(Serialization.getJsonInstance(), Serialization.getSmileInstance(), false);// false indicates to output using json, true means Smile!!

		this.executor = Threading.getNetworkExecutor();
	}

	@Deprecated
	public static void setFallbackAuthentication(AuthProvider authProvider) {
		Authenticator.setDefault(authProvider.getAuthenticator());
	}

	public ServerHandle getServer(String server, AuthProvider authProvider) {
		RestAdapter restAdapter = new RestAdapter.Builder()
				.setEndpoint(server)
				.setConverter(converter)
				.setExecutors(executor, null)
				.setRequestInterceptor(new AddAuthAndAcceptInterceptor(authProvider, JacksonConverter.ACCEPTED_MIME_TYPES))//only other way to request json is on a per Request basis... We only use retrofit for data, pictures go separately, so what the hell (Right now when in doubt the server will answer json anyway, but this is more futureproof)
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

		public RetroAuthCommunicator getAuthApi() {
			return restAdapter.create(RetroAuthCommunicator.class);
		}
	}
}

class AddAuthAndAcceptInterceptor implements RequestInterceptor {

	private static final String AUTHORIZATION = "Authorization";
	private static final String ACCEPT = "Accept";

	private final AuthProvider authProvider;
	private final String acceptMime;

	public AddAuthAndAcceptInterceptor(AuthProvider authProvider, String acceptMime) {
		this.authProvider = authProvider;
		this.acceptMime = acceptMime;
	}

	@Override
	public void intercept(RequestFacade request) {
		if (authProvider.hasUserAuth()) {
			request.addHeader(AUTHORIZATION, authProvider.getUserAuthString());
		} else if (authProvider.hasGroupAuth()) {
			request.addHeader(AUTHORIZATION, authProvider.getGroupAuthString());
		}
		request.addHeader(ACCEPT, acceptMime);
	}
}

class JacksonConverter implements Converter {
	public static final String MIME_TYPE_JSON = "application/json";
	public static final String MIME_TYPE_SMILE = "application/x-jackson-smile";
	public static final String ACCEPTED_MIME_TYPES = MIME_TYPE_JSON +", "+ MIME_TYPE_SMILE +"; q=0.8";

	private final ObjectMapper jsonMapper;
	private final ObjectMapper smileMapper;
	private final boolean outputSmile;

//	public JacksonConverter() {
//		this(new ObjectMapper());
//	}

	public JacksonConverter(ObjectMapper jsonMapper, ObjectMapper smileMapper, boolean outputSmile) {
		this.jsonMapper = jsonMapper;
		this.smileMapper = smileMapper;
		this.outputSmile = outputSmile;
	}

	@Override public Object fromBody(TypedInput body, Type type) throws ConversionException {//Technically could switch dynamically between formats here....
		try {
			if (MIME_TYPE_JSON.equals(body.mimeType())) {
				JavaType javaType = jsonMapper.getTypeFactory().constructType(type);
				return jsonMapper.readValue(body.in(), javaType);
			} else if (MIME_TYPE_SMILE.equals(body.mimeType())) {
				JavaType javaType = smileMapper.getTypeFactory().constructType(type);
				return smileMapper.readValue(body.in(), javaType);
			} else {
				throw new ConversionException("Cannot convert MIME Type: "+ body.mimeType());
			}
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
			return outputSmile? MIME_TYPE_SMILE : MIME_TYPE_JSON;
		}

		@Override
		public long length() {
			return -1;
		}

		@Override
		public void writeTo(OutputStream out) throws IOException {
			jsonMapper.writeValue(out, object);
		}
	}
}
