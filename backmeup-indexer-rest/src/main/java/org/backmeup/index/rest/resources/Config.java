package org.backmeup.index.rest.resources;

import java.net.URL;
import java.util.Random;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.backmeup.index.IndexKeepAliveTimer;
import org.backmeup.index.UserDataWorkingDir;
import org.backmeup.index.core.elasticsearch.SearchInstances;
import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.dal.RunningIndexUserConfigDao;
import org.backmeup.index.model.User;
import org.backmeup.index.serializer.Json;

@Path("config")
@Produces(MediaType.APPLICATION_JSON)
public class Config {

    private final Random random = new Random();
    @Inject
    private RunningIndexUserConfigDao dao;
    @Inject
    private SearchInstances searchInstance;
    @Inject
    private IndexKeepAliveTimer indexKeepAliveTimer;

    @GET
    @Path("/all")
    public String showAllConfigEntriesInDb() {
        return Json.serialize(dao.getAllESInstanceConfigs());
    }

    @GET
    @Path("/random")
    public String createRandomConfigAndMarkItAsAccessedInKeepAliveTimer() {
        RunningIndexUserConfig config = createRandomConfig();

        dao.save(config);
        indexKeepAliveTimer.extendTTL20(config.getUser());

        return Json.serialize(config);
    }

    private RunningIndexUserConfig createRandomConfig() {

        User userId = new User(Math.abs(random.nextLong()));
        URL hostUrl = searchInstance.getDefaultHost();
        int port = random.nextInt(65534);
        String clusterName = "user" + userId;
        String letter = "" + ((char)('a' + random.nextInt(26)));
        String dataSink = UserDataWorkingDir.getDir(userId);
        return new RunningIndexUserConfig(userId, hostUrl, port, port + 1, clusterName, letter, dataSink);
    }
}
