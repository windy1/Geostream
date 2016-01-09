package se.walkercrou.geostream.net;

import java.util.Date;

/**
 * Represents a Resource on the server. The implementing class must implement a
 * "static T parse(JSONObject)" method.
 */
public abstract class Resource {
    protected final int id;
    protected final String typeName;
    protected final Date created;

    /**
     * @see se.walkercrou.geostream.post.Post
     */
    public static final String POSTS = "posts";
    /**
     * @see se.walkercrou.geostream.post.Comment
     */
    public static final String COMMENTS = "comments";
    /**
     * @see se.walkercrou.geostream.post.Flag
     */
    public static final String FLAGS = "flags";

    protected Resource(int id, String typeName, Date created) {
        this.id = id;
        this.typeName = typeName;
        this.created = created;
    }

    public int getId() {
        return id;
    }

    public String getTypeName() {
        return typeName;
    }

    public Date getCreationDate() {
        return created;
    }
}
