package se.walkercrou.geostream.net;

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
}
