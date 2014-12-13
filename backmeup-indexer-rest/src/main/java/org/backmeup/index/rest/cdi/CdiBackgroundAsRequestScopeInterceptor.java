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
@RunRequestScoped(reason="Describes the reason why a method needs another scope.")
@Interceptor
public class CdiBackgroundAsRequestScopeInterceptor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private BoundRequestContext requestContext;

    @AroundInvoke
    public Object manageTransaction(InvocationContext ctx) throws Exception {
        try {
            log.debug("starting request scope for " + ctx.getMethod().getName());
            startRequest(ctx.getContextData());

            return ctx.proceed();

        } finally {
            endRequest(ctx.getContextData());
            log.debug("ended request scope for " + ctx.getMethod().getName());
        }
    }

    private void startRequest(Map<String, Object> requestDataStore) {
        requestContext.associate(requestDataStore);
        requestContext.activate();
    }

    /* End the request, providing the same data store as was used to start the request */

    private void endRequest(Map<String, Object> requestDataStore) {
        try {
            requestContext.invalidate();
            requestContext.deactivate();
        } finally {
            requestContext.dissociate(requestDataStore);
        }
    }

}
