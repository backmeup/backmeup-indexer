package org.backmeup.index.model;

import org.backmeup.keyserver.model.dto.TokenDTO;

/**
 * ID of the index partition, we use the Backmeup user id here.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public class User {

    private final Long id; //corresponds to the BackmeUpUserID
    private TokenDTO ksInternalToken; //used to authenticate the curr user on keyserver

    public User(Long id) {
        if (id <= -1) {
            throw new IllegalArgumentException("userID missing");
        }
        this.id = id;
    }

    /**
     * Second constructor which takes a keyserver internal token as second parameter for all calls where we need to
     * validate calls for the curr user against the keyserver
     * 
     * @param id
     * @param ksInternalToken
     */
    public User(Long id, TokenDTO ksInternalToken) {
        this(id);
        if (ksInternalToken != null) {
            this.ksInternalToken = ksInternalToken;
        }
    }

    public Long id() {
        return this.id;
    }

    public TokenDTO getKeyServerInternalToken() {
        return this.ksInternalToken;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        User other = (User) obj;
        return this.id.equals(other.id);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    public static User valueOf(String value) {
        // needed by RestEasy
        return new User(Long.valueOf(value));
    }
}
