package org.backmeup.index.utils.file;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * A simple helper database table connecting BMU userID with the the corresponding user's keyserverID This is only
 * required to query the keyserver for a user's public key. So instead of storing the user keyserver Keys as part of a
 * SharingPolicy we provide it here and update it through the BMU Service everytime a user gets created
 *
 */
@Entity
public class UserMappingHelper {

    @Id
    private Long bmuUserId;
    @Column(unique = true)
    private String ksUserId;

    public UserMappingHelper() {
    }

    public UserMappingHelper(Long bmuUserId, String keyserverUserId) {
        this.bmuUserId = bmuUserId;
        this.ksUserId = keyserverUserId;
    }

    public Long getBmuUserId() {
        return this.bmuUserId;
    }

    public void setBmuUserId(Long bmuUserId) {
        this.bmuUserId = bmuUserId;
    }

    public String getKsUserId() {
        return this.ksUserId;
    }

    public void setKsUserId(String ksUserId) {
        this.ksUserId = ksUserId;
    }

    @Override
    public String toString() {
        return this.bmuUserId + " " + this.ksUserId;
    }

}
