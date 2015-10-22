package org.backmeup.index.dal;

import org.backmeup.index.utils.file.UserMappingHelper;

/**
 * The UserMappingHelperDao contains all database relevant operations for the model class UserMappingHelper
 */
public interface UserMappingHelperDao extends BaseDao<UserMappingHelper> {

    UserMappingHelper getByBMUUserId(Long bmuUserId);

    UserMappingHelper getByKeyserverId(String ksUserId);

    @Deprecated
    void deleteAll();

}