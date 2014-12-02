package org.backmeup.index;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexManagerContextListener implements ServletContextListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        this.log.debug(">>>>> Startup IndexManager >>>>>");
        try {
            IndexManager im = IndexManager.getInstance();
            im.startupIndexManager();
        } catch (RuntimeException ex) {
            this.log.error("Error", ex);
            throw ex;
        }
        this.log.debug(">>>>> Startup IndexManager DONE >>>>>");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        this.log.debug(">>>>> Shutdown IndexManager >>>>>");
        try {
            IndexManager im = IndexManager.getInstance();
            im.shutdownIndexManager();
        } catch (RuntimeException ex) {
            this.log.error("Error", ex);
            throw ex;
        }
        this.log.debug(">>>>> Shutdown IndexManager DONE >>>>>");
    }

}
