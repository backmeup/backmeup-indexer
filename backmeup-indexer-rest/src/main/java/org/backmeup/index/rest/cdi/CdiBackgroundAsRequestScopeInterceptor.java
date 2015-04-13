package org.backmeup.index.rest.cdi;

import java.util.Map;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define an CDI intercepter for background workers to simulate request scope.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 * @see "https://docs.jboss.org/weld/reference/latest/en-US/html/contexts.html"
 */
@RunRequestScoped
@Interceptor
public class CdiBackgroundAsRequestScopeInterceptor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private BoundRequestContext requestContext;

    @AroundInvoke
    public Object manageTransaction(InvocationContext ctx) throws Exception {
        try {
            //this.log.debug("starting request scope for " + ctx.getMethod().getName());
            startRequest(ctx.getContextData());

            return ctx.proceed();

        } finally {
            endRequest(ctx.getContextData());
            //this.log.debug("ended request scope for " + ctx.getMethod().getName());
        }
    }

    private void startRequest(Map<String, Object> requestDataStore) {
        this.requestContext.associate(requestDataStore);
        this.requestContext.activate();
    }

    private void endRequest(Map<String, Object> requestDataStore) {
        try {
            this.requestContext.invalidate();
            this.requestContext.deactivate();
        } finally {
            this.requestContext.dissociate(requestDataStore);
        }
    }

}
