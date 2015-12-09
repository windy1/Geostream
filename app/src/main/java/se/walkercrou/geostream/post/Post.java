package se.walkercrou.geostream.post;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import se.walkercrou.geostream.net.ErrorCallback;
import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.request.ResourceCreateRequest;
import se.walkercrou.geostream.net.request.ResourceCreateRequest.FileValue;
import se.walkercrou.geostream.net.request.ResourceDetailRequest;
import se.walkercrou.geostream.net.request.ResourceListRequest;
import se.walkercrou.geostream.net.response.ResourceResponse;
import se.walkercrou.geostream.util.G;

/**
 * Represents a Post that a user has created with a location and an image or video.
 */
public class Post extends Resource implements Parcelable {
    // Post POST request parameters
    public static final String PARAM_ID = "id";
    public static final String PARAM_LAT = "lat";
    public static final String PARAM_LNG = "lng";
    public static final String PARAM_FILE = "media_file";
    public static final String PARAM_IS_VIDEO = "is_video";
    public static final String PARAM_CLIENT_SECRET = "client_secret";
    public static final String PARAM_COMMENTS = "comments";
    public static final String PARAM_CREATED = "created";

    public static final String BASE_FILE_NAME = "media_file.bmp";

    private int id;
    private final Location location;
    private byte[] data;
    private String fileUrl;
    private Date created;
    protected List<Comment> comments = new ArrayList<>();

    private Post(Location location, byte[] data) {
        this.location = location;
        this.data = data;
    }

    private Post(Location location, String fileUrl, int id, Date created) {
        this.location = location;
        this.fileUrl = fileUrl;
        this.id = id;
        this.created = created;
    }

    /**
     * Returns this Post's unique ID
     *
     * @return post id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the location where this post was created.
     *
     * @return location post was created at
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Returns a list of frames for the media to be posted. If an image, the returned list will be
     * of length one.
     *
     * @return media data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns true if this post is a video determined by whether the data list is of length one.
     *
     * @return true if video
     */
    public boolean isVideo() {
        return false;
    }

    /**
     * Returns the URL where this Post's media file resides.
     *
     * @return url of media file
     */
    public String getFileUrl() {
        return fileUrl;
    }

    /**
     * Sets the URL where this Post's media file resides.
     *
     * @param fileUrl url of media file
     */
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Date getCreationDate() {
        return created;
    }

    /**
     * Returns the Comments in this Post.
     *
     * @return post comments
     */
    public List<Comment> getComments() {
        return comments;
    }

    /**
     * Starts this Post's detail activity from the specified context.
     *
     * @param c context to start from
     */
    public void startActivity(Context c) {
        Intent intent = new Intent(c, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST, this);
        c.startActivity(intent);
    }

    /**
     * Creates a new comment on this post.
     *
     * @param content to comment
     * @param callback in case of error
     * @return comment
     */
    public Comment comment(String content, ErrorCallback callback) {
        return Comment.create(this, content, callback);
    }

    /**
     * Retrieves new comments from the server
     *
     * @param callback in case of error
     */
    public void refreshComments(ErrorCallback callback) {
        // send request to server
        ResourceDetailRequest<Post> request
                = new ResourceDetailRequest<>(Post.class, Resource.POSTS, id);
        ResourceResponse<Post> response = request.sendInBackground();

        // check for error
        if (response == null) {
            callback.onError(null);
            return;
        } else if (response.isError()) {
            callback.onError(response.getErrorDetail());
            return;
        }

        this.comments = response.get().comments;
    }

    /**
     * Creates and returns a new Post object and sends a creation request to the server.
     *
     * @param location of post
     * @param data     media data
     * @param callback error callback
     * @return new post object
     */
    public static Post create(Location location, String fileType, byte[] data, ErrorCallback callback) {
        // post to server
        ResourceCreateRequest<Post> request = new ResourceCreateRequest<>(Post.class, Resource.POSTS);
        request.set(PARAM_LAT, location.getLatitude())
                .set(PARAM_LNG, location.getLongitude())
                .set(PARAM_FILE, new FileValue(BASE_FILE_NAME, fileType, data))
                .set(PARAM_IS_VIDEO, false);
        ResourceResponse<Post> response = request.sendInBackground();

        G.d(response);

        // check server response
        if (response == null) {
            callback.onError(null);
            return null;
        } else if (response.isError()) {
            callback.onError(response.getErrorDetail());
            return null;
        }

        return response.get();
    }

    /**
     * Returns a list of all Posts on the server.
     *
     * @param callback error callback
     * @return list of all posts
     */
    public static List<Post> all(ErrorCallback callback) {
        // send request to server
        ResourceResponse<Post> response = new ResourceListRequest<>(Post.class, Resource.POSTS)
                .sendInBackground();
        G.d(response);

        // read response
        if (response == null) {
            callback.onError(null);
            return null;
        } else if (response.isError()) {
            callback.onError(response.getErrorDetail());
            return null;
        }

        return response.getList();
    }

    /**
     * Creates a new Post from the specified {@link JSONObject}.
     *
     * @param obj to parse
     * @return new Post
     * @throws JSONException if error with JSONObject
     */
    public static Post parse(JSONObject obj) throws JSONException, ParseException {
        // build post from object
        Location loc = new Location(G.app.name);
        loc.setLatitude(obj.getDouble(PARAM_LAT));
        loc.setLongitude(obj.getDouble(PARAM_LNG));
        String fileUrl = obj.getString(PARAM_FILE);
        int id = obj.getInt(PARAM_ID);
        Date created = G.parseDateString(obj.getString(PARAM_CREATED));
        Post post = new Post(loc, fileUrl, id, created);

        // parse comments
        JSONArray comments = obj.getJSONArray(PARAM_COMMENTS);
        for (int i = 0; i < comments.length(); i++)
            post.comments.add(Comment.parse(comments.getJSONObject(i)));

        // see if there's a client secret included
        String clientSecret = obj.optString(PARAM_CLIENT_SECRET, null);
        if (clientSecret != null)
            G.app.secrets.edit().putString(Integer.toString(id), clientSecret).commit();

        return post;
    }

    // Parcelable implementation for passing posts between activities

    public static final Parcelable.Creator<Post> CREATOR = new Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel source) {
            String fileUrl = source.readString();
            Date date;
            try {
                date = G.DATE_FORMAT.parse(source.readString());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            int id = source.readInt();
            Location location = source.readParcelable(null);
            Parcelable[] array = source.readParcelableArray(getClass().getClassLoader());

            Post post = new Post(location, fileUrl, id, date);
            for (Parcelable p : array)
                post.comments.add((Comment) p);

            return post;
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fileUrl);
        dest.writeString(G.DATE_FORMAT.format(created));

        dest.writeInt(id);

        dest.writeParcelable(location, flags);
        Parcelable[] comments = this.comments.toArray(new Parcelable[this.comments.size()]);
        dest.writeParcelableArray(comments, flags);
    }
}
