package org.backmeup.index.rest.utils;

import java.lang.annotation.Annotation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jboss.resteasy.spi.StringParameterUnmarshaller;
import org.jboss.resteasy.util.FindAnnotation;

public class DateFormatter implements StringParameterUnmarshaller<Date> {
    private SimpleDateFormat formatter; //= new SimpleDateFormat("EE MMM dd hh:mm:ss z yyyy");

    @Override
    public void setAnnotations(Annotation[] annotations) {
        DateFormat format = FindAnnotation.findAnnotation(annotations, DateFormat.class);
        this.formatter = new SimpleDateFormat(format.value());
    }

    @Override
    public Date fromString(String str) {
        Date date = null;
        try {
            date = this.formatter.parse(str);
        } catch (ParseException e) {
        }
        return date;
    }
}