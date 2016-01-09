package se.walkercrou.geostream.net;

import java.util.Date;

/**
 * Represents a Resource on the server. The implementing class must implement a
 * "static T parse(JSONObject)" method.
 */
public abstract class Resource {
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

    /**
     * Integer: A unique id for the post
     */
    public static final String PARAM_ID = "id";
    /**
     * Date: The date-time that this post was created.
     */
    public static final String PARAM_CREATED = "created";

    protected final int id;
    protected final String typeName;
    protected final Date created;

    protected Resource(int id, String typeName, Date created) {
        this.id = id;
        this.typeName = typeName;
        this.created = created;
    }

    /**
     * Returns the unique identifier of this resource.
     *
     * @return unique id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the shorthand name for this resource used by the server.
     *
     * @return shorthand name
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Returns the {@link Date} on which this resource was created.
     *
     * @return date of creation
     */
    public Date getCreationDate() {
        return created;
    }
}
