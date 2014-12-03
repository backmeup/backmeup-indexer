package org.backmeup.index.rest.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

@Provider
@Consumes({ MediaType.APPLICATION_JSON, "text/json" })
@Produces({ MediaType.APPLICATION_JSON, "text/json" })
public class JacksonJsonConfiguration extends JacksonJsonProvider {
    
    @Override
    public void writeTo(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        ObjectMapper objectMapper = locateMapper(type, mediaType);
        objectMapper.setSerializationInclusion(Inclusion.NON_NULL);

        super.writeTo(value, type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {

        ObjectMapper objectMapper = locateMapper(type, mediaType);
        objectMapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(Feature.FAIL_ON_NULL_FOR_PRIMITIVES, false);

        return super.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }
}