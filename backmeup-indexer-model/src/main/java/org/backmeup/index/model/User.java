package org.backmeup.index.model;

/**
 * ID of the index partition, we use the Backmeup user id here.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public class User {

    private final Long id;

    public User(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
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
        return id.equals(other.id);
    }

    @Override
    public String toString() {
        return id.toString();
    }

    public static User valueOf(String value) {
        // needed by RestEasy
        return new User(Long.valueOf(value));
    }
}
