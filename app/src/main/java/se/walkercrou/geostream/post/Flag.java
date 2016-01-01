package se.walkercrou.geostream.post;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import se.walkercrou.geostream.net.ErrorCallback;
import se.walkercrou.geostream.net.Resource;
import se.walkercrou.geostream.net.request.ResourceCreateRequest;
import se.walkercrou.geostream.net.response.ResourceResponse;
import se.walkercrou.geostream.util.G;

/**
 * Represents a "report flag" for a given Post. These flags are sent to the server to be reviewed
 * by moderators. If a post is flagged 5 times or more, it is taken down automatically.
 */
public class Flag extends Resource {
    /**
     * Integer: {@link Post} ID that this flag belongs to.
     */
    public static final String PARAM_POST = "post";
    /**
     * Date: The date this flag was created.
     */
    public static final String PARAM_CREATED = "created";
    /**
     * String: The reason that the {@link Post} was flagged.
     * @see Reason
     */
    public static final String PARAM_REASON = "reason";

    private final Reason reason;
    private final Date created;

    private Flag(Reason reason, Date created) {
        this.reason = reason;
        this.created = created;
    }

    /**
     * Returns the {@link Reason} for the {@link Post} that was flagged.
     *
     * @return reason for flagging
     */
    public Reason getReason() {
        return reason;
    }

    /**
     * Returns the {@link Date} when this flag was created.
     *
     * @return creation date
     */
    public Date getCreationDate() {
        return created;
    }

    /**
     * Creates a new flag for the specified {@link Post} and sends it to the server for review. This
     * method is protected for consistency. Use {@link Post#flag(Context, Reason, ErrorCallback)}
     * instead.
     *
     * @param c context
     * @param post to flag
     * @param reason for flagging
     * @param callback in case of error
     * @return flag
     * @throws IOException
     */
    protected static Flag create(Context c, Post post, Reason reason, ErrorCallback callback)
            throws IOException {
        // send request to server
        ResourceCreateRequest<Flag> request
                = new ResourceCreateRequest<>(c, Flag.class, Resource.FLAGS);
        request.set(PARAM_POST, post.getId()).set(PARAM_REASON, reason.name);
        ResourceResponse<Flag> response = request.sendInBackground();

        // check for error
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
     * Creates a new Flag from the specified {@link JSONObject}.
     *
     * @param c context
     * @param json to parse
     * @return flag from json
     * @throws JSONException
     */
    public static Flag parse(Context c, JSONObject json) throws JSONException, ParseException {
        return new Flag(Reason.nameMap.get(json.getString(PARAM_REASON)),
                G.parseDateString(json.getString(PARAM_CREATED)));
    }

    /**
     * Represents a reason for flagging a {@link Post}.
     *
     * @see <a href="https://play.google.com/about/developer-content-policy.html">
     *         https://play.google.com/about/developer-content-policy.html
     *     </a>
     */
    public enum Reason {
        /**
         * The post contains content that is deemed to be inappropriate. This includes sexually
         * explicit material, hate speech, sensitive events, intellectual property,
         * illegal activities, etc.
         */
        INAPPROPRIATE_CONTENT("IC"),
        /**
         * The post contains content that violates the user's or another's privacy. This includes
         * personal or confidential information, etc.
         */
        PRIVACY_VIOLATION("PV"),
        /**
         * The post contains content that depicts violence, bullying, or targeting of others.
         */
        VIOLENCE_OR_BULLYING("VB"),
        /**
         * This post contains content that includes deception, impersonation, or is an unauthorized
         * advertisement.
         */
        SPAM("SP");

        private static final Map<String, Reason> nameMap = new HashMap<>();

        static {
            for (Reason reason : Reason.values())
                nameMap.put(reason.name, reason);
        }

        private final String name;

        Reason(String name) {
            this.name = name;
        }
    }
}
