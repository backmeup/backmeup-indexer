package org.backmeup.index.dal.jpa;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.backmeup.index.dal.IndexManagerDao;
import org.backmeup.index.db.RunningIndexUserConfig;

/**
 * The ProfileDaoImpl realizes the ProfileDao interface with JPA specific
 * operations.
 * 
 * @author fschoeppl
 * 
 */
public class IndexManagerDaoImpl extends
		BaseDaoImpl<RunningIndexUserConfig> implements IndexManagerDao {

	public IndexManagerDaoImpl(EntityManager em) {
		super(em);
	}

	@Override
	public RunningIndexUserConfig findConfigByUserId(Long userID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<RunningIndexUserConfig> getAllESInstanceConfigs() {
		Query q = em.createQuery("SELECT * FROM IndexRunningInstanceConfig");
		List<RunningIndexUserConfig> indexConfig = q.getResultList();
		if (indexConfig != null && indexConfig.size() > 0) {
			return indexConfig;
		} else {
			return null;
		}
	}

	@Override
	public RunningIndexUserConfig findConfigByHttpPort(int httpPort) {
		Query q = em
				.createQuery("SELECT u FROM IndexRunningInstanceConfig u WHERE httpPort = :httpport");
		q.setParameter("httpport", httpPort);
		List<RunningIndexUserConfig> indexConfig = q.getResultList();
		RunningIndexUserConfig u = indexConfig.size() > 0 ? indexConfig
				.get(0) : null;
		return u;
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
}
