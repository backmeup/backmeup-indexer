package org.backmeup.index.rest.resources;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.dal.RunningIndexUserConfigDao;
import org.backmeup.index.model.User;
import org.backmeup.index.serializer.Json;

@Path("config")
@Produces(MediaType.APPLICATION_JSON)
public class Config {

    @Inject
    private RunningIndexUserConfigDao dao;

    @GET
    @Path("/all")
    public String all() {
        return Json.serialize(dao.getAllESInstanceConfigs());
    }

    @GET
    @Path("/random")
    public String createRandom() throws MalformedURLException {
        RunningIndexUserConfig config = createRandomConfig();
        dao.save(config);
        return Json.serialize(config);
    }

    private RunningIndexUserConfig createRandomConfig() throws MalformedURLException {
        Random random = new Random();

        User userId = new User(Math.abs(random.nextLong()));
        URL hostUrl = new URL("http://1.com/");
        int port = random.nextInt(65534);
        String clusterName = "user" + userId;
        String letter = "" + ('a' + random.nextInt(26));
        return new RunningIndexUserConfig(userId, hostUrl, port, port + 1, clusterName, letter, "/tmp");
    }
}
