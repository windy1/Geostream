package se.walkercrou.geostream.post;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

import se.walkercrou.geostream.net.ErrorCallback;
import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.request.ResourceCreateRequest;
import se.walkercrou.geostream.net.response.ResourceResponse;
import se.walkercrou.geostream.util.G;

/**
 * Represents a comment within a Post
 */
public class Comment extends Resource implements Parcelable {
    private final String content;
    private final Date created;

    public static final String PARAM_POST = "post";
    public static final String PARAM_CONTENT = "content";
    public static final String PARAM_CREATED = "created";

    private Comment(String content, Date created) {
        this.content = content;
        this.created = created;
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

    /**
     * Creates a new Comment from the specified {@link JSONObject}.
     *
     * @param obj to parse
     * @return new comment
     * @throws JSONException if error with json
     */
    public static Comment parse(JSONObject obj) throws JSONException, ParseException {
        return new Comment(obj.getString(PARAM_CONTENT),
                G.parseDateString(obj.getString(PARAM_CREATED)));
    }

    /**
     * Creates a new Comment on the specified Post.
     *
     * @param post to comment on
     * @param content of comment
     * @param callback to call if error
     * @return comment if successful, null otherwise
     */
    public static Comment create(Post post, String content, ErrorCallback callback) {
        // send request to server
        ResourceCreateRequest<Comment> request
                = new ResourceCreateRequest<>(Comment.class, Resource.COMMENTS);
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

    @Override
    public String toString() {
        return content;
    }

    // Parcelable implementation

    public static Parcelable.Creator<Comment> CREATOR = new Creator<Comment>() {
        @Override
        public Comment createFromParcel(Parcel source) {
            try {
                return new Comment(source.readString(), G.DATE_FORMAT.parse(source.readString()));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
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
        dest.writeString(content);
        dest.writeString(G.DATE_FORMAT.format(created));
    }
}
