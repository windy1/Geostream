package se.walkercrou.geostream.post;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import se.walkercrou.geostream.net.ErrorCallback;
import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.request.ResourceCreateRequest;
import se.walkercrou.geostream.net.request.ResourceCreateRequest.MediaData;
import se.walkercrou.geostream.net.request.ResourceDeleteRequest;
import se.walkercrou.geostream.net.request.ResourceDetailRequest;
import se.walkercrou.geostream.net.request.ResourceListRequest;
import se.walkercrou.geostream.net.response.ResourceResponse;
import se.walkercrou.geostream.util.G;

/**
 * Represents a Post that a user has created with a location and an image or video.
 */
public class Post extends Resource implements Parcelable {
    /**
     * Integer: A unique id for the post
     */
    public static final String PARAM_ID = "id";
    /**
     * Float: The latitudinal coordinate where this post was taken.
     */
    public static final String PARAM_LAT = "lat";
    /**
     * Float: The longitudinal coordinate where this post was taken.
     */
    public static final String PARAM_LNG = "lng";
    /**
     * File (byte array): The file associated with the post.
     */
    public static final String PARAM_MEDIA_FILE = "media_file";
    /**
     * Integer: The amount of hours this Post should be available.
     */
    public static final String PARAM_LIFETIME = "lifetime";
    /**
     * String: The secret returned by the server on post creation, used for deletion.
     */
    public static final String PARAM_CLIENT_SECRET = "client_secret";
    /**
     * {@link Comment} array: The comments associated with this post
     */
    public static final String PARAM_COMMENTS = "comments";
    /**
     * Date: The date-time that this post was created.
     */
    public static final String PARAM_CREATED = "created";
    /**
     * Float: The lower latitude coordinate for retrieving posts within a range.
     */
    public static final String PARAM_FROM_LAT = "fromLat";
    /**
     * The upper latitude coordinate for retrieving posts within a range.
     */
    public static final String PARAM_TO_LAT = "toLat";
    /**
     * The lower longitude coordinate for retrieving posts within a range.
     */
    public static final String PARAM_FROM_LNG = "fromLng";
    /**
     * The upper latitude coordinate for retrieving posts within a range.
     */
    public static final String PARAM_TO_LNG = "toLng";

    /**
     * API method that retrieve posts within a specified range.
     */
    public static final String METHOD_RANGE = "range";

    /**
     * What we tell the server to name the file. Absolutely arbitrary. May also be renamed by the
     * server.
     */
    public static final String BASE_FILE_NAME = "media_file";
    /**
     * File extension used when the Post's media is an image.
     */
    public static final String IMAGE_FILE_EXTENSION = "bmp";
    /**
     * File extension used when the Post's media is a video.
     */
    public static final String VIDEO_FILE_EXTENSION = "mp4";

    /**
     * Shorthand for server use.
     */
    public static final String TYPE_NAME = "PST";

    private final Location location;
    private final String fileUrl;
    protected List<Comment> comments = new ArrayList<>();

    private Post(int id, Location location, String fileUrl, Date created) {
        super(id, TYPE_NAME, created);
        this.location = location;
        this.fileUrl = fileUrl;
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
     * Returns the URL where this Post's media file resides. Note: Could contain the server URL in
     * the beginning or just the on the server.
     *
     * @return url of media file
     */
    public String getMediaUrl() {
        return fileUrl;
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
     * Returns true if this post is hidden for this device.
     *
     * @return true if hidden
     */
    public boolean isHidden() {
        return G.app.hiddenPosts.getBoolean(Integer.toString(id), false);
    }

    /**
     * Sets whether this post should be hidden for this device.
     *
     * @param hidden true if should hide
     */
    public void setHidden(boolean hidden) {
        G.app.hiddenPosts.edit().putBoolean(Integer.toString(id), hidden).commit();
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
     * @param c context
     * @param content to comment
     * @param callback in case of error
     * @return comment
     */
    public Comment comment(Context c, String content, ErrorCallback callback) throws IOException {
        return Comment.create(c, this, content, callback);
    }

    /**
     * Creates a new flag on this post and sends it to the server for review.
     *
     * @param c context
     * @param reason for flagging
     * @param callback in case of error
     * @return flag
     * @throws IOException
     */
    public Flag flag(Context c, Flag.Reason reason, ErrorCallback callback) throws IOException {
        return Flag.create(c, this, reason, callback);
    }

    /**
     * Retrieves new comments from the server
     *
     * @param callback in case of error
     */
    public void refreshComments(Context c, ErrorCallback callback) throws IOException {
        // send request to server
        ResourceDetailRequest<Post> request
                = new ResourceDetailRequest<>(c, Post.class, Resource.POSTS, id);
        ResourceResponse<Post> response = request.sendInBackground();

        if (!ResourceResponse.check(response, callback))
            return;

        this.comments = response.get().comments;
    }

    /**
     * Deletes this Post on the server.
     */
    public boolean delete(Context c, String clientSecret, ErrorCallback callback)
            throws IOException {
        ResourceDeleteRequest<Post> request
                = new ResourceDeleteRequest<>(c, Post.class, Resource.POSTS, id, clientSecret);
        ResourceResponse<Post> response = request.sendInBackground();
        return ResourceResponse.check(response, callback);

    }

    /**
     * Creates and returns a new Post object and sends a creation request to the server.
     *
     * @param location of post
     * @param data     media data
     * @param callback error callback
     * @return new post object
     */
    public static Post create(Context c, Location location, int lifetime, MediaData data,
                              ErrorCallback callback) throws IOException {
        // post to server
        ResourceCreateRequest<Post> request
                = new ResourceCreateRequest<>(c, Post.class, Resource.POSTS);
        request.set(PARAM_LAT, location.getLatitude())
                .set(PARAM_LNG, location.getLongitude())
                .set(PARAM_LIFETIME, lifetime)
                .set(PARAM_MEDIA_FILE, data);
        ResourceResponse<Post> response = request.sendInBackground();

        G.d(response);

        if (!ResourceResponse.check(response, callback))
            return null;
        return response.get();
    }

    /**
     * Returns a list of Posts within the specified range.
     *
     * @param c context
     * @param fromLat lower latitude
     * @param toLat upper latitude
     * @param fromLng lower longitude
     * @param toLng upper longitude
     * @param callback in case of error
     * @return list of posts within range
     * @throws IOException
     */
    public static List<Post> range(Context c, double fromLat, double toLat, double fromLng,
                                   double toLng, ErrorCallback callback) throws IOException {
        // send request to server
        ResourceListRequest<Post> request
                = new ResourceListRequest<>(c, Post.class, Resource.POSTS, "range");
        request.set(PARAM_FROM_LAT, fromLat)
                .set(PARAM_TO_LAT, toLat)
                .set(PARAM_FROM_LNG, fromLng)
                .set(PARAM_TO_LNG, toLng);
        ResourceResponse<Post> response = request.sendInBackground();
        if (!ResourceResponse.check(response, callback))
            return null;
        return response.getList();
    }

    /**
     * Returns a list of all Posts on the server.
     *
     * @param callback error callback
     * @return list of all posts
     */
    public static List<Post> all(Context c, ErrorCallback callback) throws IOException {
        // send request to server
        ResourceResponse<Post> response = new ResourceListRequest<>(c, Post.class, Resource.POSTS)
                .sendInBackground();
        G.d(response);
        if (!ResourceResponse.check(response, callback))
            return null;
        return response.getList();
    }

    /**
     * Creates a new Post from the specified {@link JSONObject}.
     *
     * @param obj to parse
     * @return new Post
     * @throws JSONException if error with JSONObject
     */
    public static Post parse(Context c, JSONObject obj) throws JSONException, ParseException {
        // build post from object
        Location loc = new Location(G.app.name);
        loc.setLatitude(obj.getDouble(PARAM_LAT));
        loc.setLongitude(obj.getDouble(PARAM_LNG));
        String fileUrl = obj.getString(PARAM_MEDIA_FILE);
        int id = obj.getInt(PARAM_ID);
        Date created = G.parseDateString(obj.getString(PARAM_CREATED));
        Post post = new Post(id, loc, fileUrl, created);

        // parse comments
        JSONArray comments = obj.getJSONArray(PARAM_COMMENTS);
        for (int i = 0; i < comments.length(); i++)
            post.comments.add(Comment.parse(c, comments.getJSONObject(i)));

        // see if there's a client secret included, if so, save it to the device for later use
        // the client secret is only returned on initial post creation
        String clientSecret = obj.optString(PARAM_CLIENT_SECRET, null);
        if (clientSecret != null)
            G.app.postSecrets.edit().putString(Integer.toString(id), clientSecret).commit();

        return post;
    }

    /**
     * Generates a file name for any post.
     *
     * @param video true if the post is a video
     * @return file name
     */
    public static String fileName(boolean video) {
        return BASE_FILE_NAME + '.' + (video ? VIDEO_FILE_EXTENSION : IMAGE_FILE_EXTENSION);
    }

    /*
     * -- Parcelable implementation for passing posts between activities --
     *
     * Order:
     * 1. Integer: ID
     * 2. String: File URL
     * 3. String: Creation date
     * 4. Parcelable: Location
     * 5. Parcelable array: Comments
     */

    public static final Parcelable.Creator<Post> CREATOR = new Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel source) {
            int id = source.readInt();
            String fileUrl = source.readString();

            Date date;
            try {
                date = G.STANDARD_DATE_FORMAT.parse(source.readString());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            Location location = source.readParcelable(null);
            Parcelable[] comments = source.readParcelableArray(getClass().getClassLoader());

            Post post = new Post(id, location, fileUrl, date);
            for (Parcelable comment : comments)
                post.comments.add((Comment) comment);

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
        dest.writeInt(id);
        dest.writeString(fileUrl);
        dest.writeString(G.STANDARD_DATE_FORMAT.format(created));

        dest.writeParcelable(location, flags);
        Parcelable[] comments = this.comments.toArray(new Parcelable[this.comments.size()]);
        dest.writeParcelableArray(comments, flags);
    }

    // --------------------------------------------------------------------
}
