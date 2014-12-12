package org.backmeup.index.dal.jpa;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.dal.IndexManagerDao;
import org.backmeup.index.model.User;

@RequestScoped
public class IndexManagerDaoImpl extends BaseDaoImpl<RunningIndexUserConfig>
		implements IndexManagerDao {

	private static final String TABLENAME = RunningIndexUserConfig.class
			.getSimpleName();

	public IndexManagerDaoImpl(EntityManager em) {
		super(em);
	}

	@Override
	public RunningIndexUserConfig findConfigByUser(User userID) {
	    TypedQuery<RunningIndexUserConfig> q = createTypedQuery("SELECT u FROM " + TABLENAME
				+ " u WHERE u.userId = :userId");
		q.setParameter("userId", userID.id());
		return executeQuerySelectFirst(q);
	}

	@Override
	public RunningIndexUserConfig findConfigByHttpPort(URL host) {
		if ((host.getProtocol() != null) && (host.getHost() != null)
				&& (host.getPort() > -1)) {

			String url = host.getProtocol() + "://" + host.getHost();
			Integer httpPort = host.getPort();

			TypedQuery<RunningIndexUserConfig> q = createTypedQuery("SELECT u FROM "
							+ TABLENAME
							+ " u WHERE u.httpPort = :httpport and u.hostaddress =:hostaddr");
			System.out.println("running findConfigByHttpPort query for: "
					+ host);
			q.setParameter("httpport", httpPort);
			q.setParameter("hostaddr", url);
			return executeQuerySelectFirst(q);

		} 
			
		return null;
	}

	@Override
	public RunningIndexUserConfig findConfigByClusterName(String clusterName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RunningIndexUserConfig findConfigByDriveLetter(String driveLetter) {
		// TODO Auto-generated method stub
		return null;
	}

	private RunningIndexUserConfig executeQuerySelectFirst(TypedQuery<RunningIndexUserConfig> q) {
		List<RunningIndexUserConfig> indexConfig = executeQuery(q);
        return indexConfig.size() > 0 ? indexConfig.get(0) : null;
	}

    private List<RunningIndexUserConfig> executeQuery(TypedQuery<RunningIndexUserConfig> q) {
        List<RunningIndexUserConfig> indexConfig = q.getResultList();
        if (indexConfig != null && indexConfig.size() > 0) {
            return indexConfig;
        } 
        return new ArrayList<>();
    }

	@Override
	public List<RunningIndexUserConfig> getAllESInstanceConfigs() {
	    TypedQuery<RunningIndexUserConfig> q = createTypedQuery("SELECT u FROM " + TABLENAME + " u");
        return executeQuery(q);
	}

	@Override
	public List<RunningIndexUserConfig> getAllESInstanceConfigs(URL url) {
		if (url != null) {
		    TypedQuery<RunningIndexUserConfig> q = createTypedQuery("SELECT u FROM " + TABLENAME
                    		+ " u WHERE u.hostaddress = :instance");
			q.setParameter("instance", url.toExternalForm());
			return executeQuery(q);
		} 

		return new ArrayList<>();
	}

    private TypedQuery<RunningIndexUserConfig> createTypedQuery(String sql) {
        return this.entityManager.createQuery(sql, RunningIndexUserConfig.class);
    }

    @Override
    public void deleteAll() {
        this.entityManager.createQuery("DELETE FROM +" + TABLENAME).executeUpdate();
    }

}
