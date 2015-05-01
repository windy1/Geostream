package se.walkercrou.geostream.net;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.walkercrou.geostream.App;

/**
 * Represents a Post that a user has created with a location and an image or video.
 */
public class Post implements Parcelable {
    public static final String PARAM_LAT = "lat";
    public static final String PARAM_LNG = "lng";
    public static final String PARAM_USER = "user";
    public static final String PARAM_FILE = "media_file";
    public static final String PARAM_IS_VIDEO = "is_video";

    public static final String FILE_NAME = "media_file.bmp";

    private static final Map<Marker, Post> mappedPosts = new HashMap<>();

    private final Location location;
    private byte[] data;
    private String fileUrl;
    private Bitmap image;

    public Post(Location location, byte[] data) {
        this.location = location;
        this.data = data;
    }

    public Post(Location location, String fileUrl) {
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

    public boolean downloadImage(Context c) {
        // TODO: download image from media url
        return true;
    }

    /**
     * Adds this Post to the specified {@link GoogleMap}.
     *
     * @param map to place post on
     */
    public void placeOnMap(GoogleMap map) {
        Marker marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(location.getLatitude(), location.getLongitude())));
        mappedPosts.put(marker, this);
    }

    /**
     * Returns a CREATE {@link Request} for this Post.
     *
     * @param c context
     * @return CREATE request
     */
    public Request createRequest(Context c) {
        SharedPreferences prefs = App.getSharedPreferences(c);
        int userId = prefs.getInt(App.PREF_USER_ID, 0);
        String username = prefs.getString(App.PREF_USER_NAME, null);
        String password = prefs.getString(App.PREF_USER_PASSWORD, null);
        return new Request(Request.METHOD_POST, Request.URL_POST_LIST)
                .set(PARAM_LAT, location.getLatitude())
                .set(PARAM_LNG, location.getLongitude())
                .set(PARAM_USER, userId)
                .set(PARAM_FILE, new FileValue(FILE_NAME, data))
                .set(PARAM_IS_VIDEO, isVideo())
                .setAuthorization(username, password);
    }

    /**
     * Returns a LIST {@link Request} for all Posts.
     *
     * @param c context
     * @return request to LIST all posts
     */
    public static Request listRequest(Context c) {
        SharedPreferences prefs = App.getSharedPreferences(c);
        String username = prefs.getString(App.PREF_USER_NAME, null);
        String password = prefs.getString(App.PREF_USER_PASSWORD, null);
        return new Request(Request.METHOD_GET, Request.URL_POST_LIST)
                .setAuthorization(username, password);
    }

    /**
     * Returns the Post linked to the specified map {@link Marker}.
     *
     * @param marker to get post for
     * @return post mapped to marker
     */
    public static Post getPostFor(Marker marker) {
        return mappedPosts.get(marker);
    }

    /**
     * Returns a list of posts from the given JSON input.
     *
     * @param array of posts in json format
     * @return post objects
     */
    public static List<Post> parse(JSONArray array) {
        List<Post> posts = new ArrayList<>(array.length());
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonPost = array.getJSONObject(i);
                Location loc = new Location(App.getName());
                loc.setLatitude(jsonPost.getDouble(PARAM_LAT));
                loc.setLongitude(jsonPost.getDouble(PARAM_LNG));
                posts.add(new Post(loc, jsonPost.getString(PARAM_FILE)));
            }
        } catch (JSONException e) {
            App.e("An error occurred while parsing post data", e);
        }
        return posts;
    }

    // Parcelable implementation for passing posts between activities

    public static final Parcelable.Creator<Post> CREATOR = new Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel source) {
            Location location = new Location(App.getName());
            location.setLatitude(source.readDouble());
            location.setLongitude(source.readDouble());
            return new Post(location, source.readString());
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
        dest.writeDouble(location.getLatitude());
        dest.writeDouble(location.getLongitude());
        dest.writeString(fileUrl);
    }
}
