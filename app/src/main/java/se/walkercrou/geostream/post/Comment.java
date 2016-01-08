package se.walkercrou.geostream.post;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import se.walkercrou.geostream.net.ErrorCallback;
import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.request.ResourceCreateRequest;
import se.walkercrou.geostream.net.request.ResourceDeleteRequest;
import se.walkercrou.geostream.net.response.ResourceResponse;
import se.walkercrou.geostream.util.G;

/**
 * Represents a comment within a Post
 */
public class Comment extends Resource implements Parcelable {
    /**
     * Integer: Unique identifier of comment
     */
    public static final String PARAM_ID = "id";
    /**
     * Integer: Post ID that this comment belongs to.
     */
    public static final String PARAM_POST = "post";
    /**
     * String: The actual content of the comment.
     */
    public static final String PARAM_CONTENT = "content";
    /**
     * Date: The date this comment was created.
     */
    public static final String PARAM_CREATED = "created";
    /**
     * String: Client secret for modifying comment in the future
     */
    public static final String PARAM_CLIENT_SECRET = "client_secret";

    private final int id;
    private final String content;
    private final Date created;

    private Comment(int id, String content, Date created) {
        this.id = id;
        this.content = content;
        this.created = created;
    }

    /**
     * Returns this comments unique identifier.
     *
     * @return unique id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the content string of this comment.
     *
     * @return content string
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the date when this comment was created.
     *
     * @return creation date
     */
    public Date getCreationDate() {
        return created;
    }

    public boolean delete(Context c, String clientSecret, ErrorCallback callback)
            throws IOException {
        ResourceDeleteRequest<Comment> request = new ResourceDeleteRequest<>(c, Comment.class,
                Resource.COMMENTS, id, clientSecret);
        ResourceResponse<Comment> response = request.sendInBackground();

        if (response == null) {
            callback.onError(null);
            return false;
        } else if (response.isError()) {
            callback.onError(response.getErrorDetail());
            return false;
        }

        return true;
    }

    /**
     * Creates a new Comment for the specified Post. This method is protected for consistency. Use
     * {@link Post#comment(Context, String, ErrorCallback)} instead.
     *
     * @param post to comment on
     * @param content of comment
     * @param callback error callback
     * @return newly created comment
     * @see Post#comment(Context, String, ErrorCallback)
     */
    protected static Comment create(Context c, Post post, String content, ErrorCallback callback)
            throws IOException {
        // send request to server
        ResourceCreateRequest<Comment> request
                = new ResourceCreateRequest<>(c, Comment.class, Resource.COMMENTS);
        request.set(PARAM_POST, post.getId()).set(PARAM_CONTENT, content);
        ResourceResponse<Comment> response = request.sendInBackground();

        // check for error
        if (response == null) {
            callback.onError(null);
            return null;
        } else if (response.isError()) {
            callback.onError(response.getErrorDetail());
            return null;
        }

        // create comment and add it to the post
        Comment comment = response.get();
        post.comments.add(comment);
        return comment;
    }

    /**
     * Creates a new Comment from the specified {@link JSONObject}.
     *
     * @param obj to parse
     * @return new comment
     * @throws JSONException if error with json
     */
    public static Comment parse(Context c, JSONObject obj) throws JSONException, ParseException {
        int id = obj.getInt(PARAM_ID);

        // check for client secret and save if present
        String clientSecret = obj.optString(PARAM_CLIENT_SECRET, null);
        if (clientSecret != null)
            G.app.commentSecrets.edit().putString(Integer.toString(id), clientSecret).commit();

        return new Comment(id, obj.getString(PARAM_CONTENT),
                G.parseDateString(obj.getString(PARAM_CREATED)));
    }

    @Override
    public String toString() {
        return content;
    }

    /*
     * -- Parcelable implementation --
     * Allows comments to be passed between activities along with their posts.
     *
     * Order:
     * 1. Integer: ID
     * 2. String: Content
     * 3. String: Creation date
     */

    public static Parcelable.Creator<Comment> CREATOR = new Creator<Comment>() {
        @Override
        public Comment createFromParcel(Parcel source) {
            int id = source.readInt();
            String content = source.readString();

            Date date;
            try {
                date = G.STANDARD_DATE_FORMAT.parse(source.readString());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            return new Comment(id, content, date);
        }

        @Override
        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(content);
        dest.writeString(G.STANDARD_DATE_FORMAT.format(created));
    }

    // -------------------------------
}
