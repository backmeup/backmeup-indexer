package org.backmeup.index;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * TODO currently not active, as not deployed as servlet
 * change deployment unit to war, add web.xml
 * @see http://stackoverflow.com/questions/3838217/using-cdi-weld-in-tomcat-with-simple-pojo-classes 
 */

public class IndexManagerContextListener implements ServletContextListener {
    private final Logger log = LoggerFactory.getLogger(IndexManagerContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        this.log.debug(">>>>> Startup IndexManager >>>>>");
        try {
            IndexManager im = IndexManager.getInstance();
            im.startupIndexManager();
        } catch (Exception ex) {
            this.log.error("Error (nothing injected?)", ex);
        }
        this.log.debug(">>>>> Startup IndexManager DONE >>>>>");
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        this.log.debug(">>>>> Shutdown IndexManager >>>>>");
        try {
            IndexManager im = IndexManager.getInstance();
            im.shutdownIndexManager();
        } catch (Exception ex) {
            this.log.error("Error (nothing injected?)", ex);
        }
        this.log.debug(">>>>> Shutdown IndexManager DONE >>>>>");
    }

}
