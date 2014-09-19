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
import java.net.HttpURLConnection;

import retrofit.RestAdapter;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * Created by Ramon on 19.09.2014.
 */
public class RetroSetup {

	public RetroSetup(String server) {
		RestAdapter restAdapter = new RestAdapter.Builder()
				.setEndpoint(server)
				.setConverter(new JacksonConverter())
				.build();

		HttpURLConnection
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
