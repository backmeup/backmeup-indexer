package org.backmeup.index.dal.jpa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

/**
 * Define an CDI interceptor for transactions.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 * @see "https://docs.jboss.org/weld/reference/1.0.0/en-US/html/interceptors.html#d0e3451"
 */
@InterceptorBinding
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
}