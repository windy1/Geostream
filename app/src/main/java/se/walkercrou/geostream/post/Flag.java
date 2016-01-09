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
    public static final String PARAM_ID = "id";
    public static final String PARAM_RESOURCE_TYPE = "resource_type";
    public static final String PARAM_RESOURCE_ID = "resource_id";
    /**
     * Date: The date this flag was created.
     */
    public static final String PARAM_CREATED = "created";
    /**
     * String: The reason that the {@link Post} was flagged.
     * @see Reason
     */
    public static final String PARAM_REASON = "reason";

    /**
     * Shorthand for server use
     */
    public static final String TYPE_NAME = "FLG";

    private final Reason reason;

    private Flag(int id, Reason reason, Date created) {
        super(id, TYPE_NAME, created);
        this.reason = reason;
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
     * Creates a new flag for the specified {@link Resource} and sends it to the server for review.
     * This method is protected for consistency.
     *
     * @param c context
     * @param resource to flag
     * @param reason for flagging
     * @param callback in case of error
     * @return flag
     * @throws IOException
     */
    protected static Flag create(Context c, Resource resource, Reason reason,
                                 ErrorCallback callback) throws IOException {
        // send request to server
        G.d("creating flag for resource: " + resource.getTypeName() + '/' + resource.getId());
        ResourceCreateRequest<Flag> request
                = new ResourceCreateRequest<>(c, Flag.class, Resource.FLAGS);
        request.set(PARAM_RESOURCE_TYPE, resource.getTypeName())
                .set(PARAM_RESOURCE_ID, resource.getId())
                .set(PARAM_REASON, reason);
        ResourceResponse<Flag> response = request.sendInBackground();

        if (!ResourceResponse.check(response, callback))
            return null;
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
        return new Flag(json.getInt(PARAM_ID), Reason.nameMap.get(json.getString(PARAM_REASON)),
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

        @Override
        public String toString() {
            return name;
        }
    }
}
