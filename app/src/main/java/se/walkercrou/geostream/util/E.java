package se.walkercrou.geostream.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;

import se.walkercrou.geostream.R;

import static android.content.DialogInterface.OnClickListener;

/**
 * Helper class for building error dialogs to display.
 */
public final class E {
    private E() {
    }

    /**
     * Returns a connection error {@link AlertDialog}.
     *
     * @param c              context
     * @param tryAgainAction button action
     * @return alert dialog
     */
    public static AlertDialog connection(Context c, OnClickListener tryAgainAction) {
        G.e("could not connect to internet");
        String msg = c.getString(R.string.error_no_connection, G.app.name);
        return builder(c, msg).setPositiveButton(R.string.action_try_again, tryAgainAction).create();
    }

    /**
     * Returns a dialog that explains that an internal error has occurred.
     *
     * @param c context
     * @return error dialog
     */
    public static AlertDialog internal(Activity c, String error) {
        G.e("internal error. server says: \"" + error + "\"");
        String msg = c.getString(R.string.error_internal, G.app.name);
        return builder(c, msg).setPositiveButton(R.string.action_ok, ((d, w) -> c.finish())).show();
    }

    /**
     * Returns a dialog to display when an error occurs while attempting to send a post.
     *
     * @param c context
     * @return alert dialog
     */
    public static AlertDialog postSend(Context c) {
        G.e("could not send post to server");
        return builder(c, R.string.error_send_post)
                .setPositiveButton(R.string.action_ok, (d, w) -> {})
                .create();
    }

    /**
     * Returns a dialog to display when an error occurs while attempting to report a post.
     *
     * @param c context
     * @return alert dialog
     */
    public static AlertDialog reportPost(Context c) {
        G.e("could not report post");
        return builder(c, R.string.error_report)
                .setPositiveButton(R.string.action_ok, (d, w) -> {})
                .create();
    }

    public static AlertDialog reportComment(Context c) {
        G.e("could not report comment");
        return builder(c, R.string.error_report_comment)
                .setPositiveButton(R.string.action_ok, (d, w) -> {})
                .create();
    }

    /**
     * Returns a dialog to display when an error occurs while attempting to delete a post.
     *
     * @param c context
     * @return alert dialog
     */
    public static AlertDialog deletePost(Context c) {
        G.e("could not discard post");
        return builder(c, R.string.error_discard)
                .setPositiveButton(R.string.action_ok, (d, w) -> {})
                .create();
    }

    /**
     * Returns a dialog to display when an error occurs while attempting to delete a comment.
     *
     * @param c context
     * @return alert dialog
     */
    public static AlertDialog deleteComment(Context c) {
        G.e("could not delete comment");
        return builder(c, R.string.error_delete_comment)
                .setPositiveButton(R.string.action_ok, (d, w) -> {})
                .create();
    }

    /**
     * Returns a dialog to display when an error occurs while attempting to comment on a post.
     *
     * @param c context
     * @return alert dialog
     */
    public static AlertDialog comment(Context c) {
        G.e("could not comment");
        return builder(c, R.string.error_comment)
                .setPositiveButton(R.string.action_ok, (d, w) -> {})
                .create();
    }

    /**
     * Returns a dialog to display when an error occurs while opening the camera.
     *
     * @param c context
     * @return alert dialog
     */
    public static AlertDialog cameraOpen(final Activity c) {
        G.e("could not open camera");
        String msg = c.getString(R.string.error_no_camera, G.app.name);
        return builder(c, msg).setPositiveButton(R.string.action_back, (d, w) -> c.finish()).create();
    }

    /**
     * Returns a dialog to display when an error occurs while trying to get the device's location.
     *
     * @param c              context
     * @param tryAgainAction action if user presses try again
     * @return alert dialog
     */
    public static AlertDialog location(Context c, OnClickListener tryAgainAction) {
        G.e("could not determine location");
        String msg = c.getString(R.string.error_no_location, G.app.name);
        return builder(c, msg).setPositiveButton(R.string.action_try_again, tryAgainAction).create();
    }

    /**
     * Returns the dialog to display when an error occurs while creating the map.
     *
     * @param c activity
     * @return dialog to display
     */
    public static AlertDialog map(Activity c) {
        G.e("could not create map");
        String msg = c.getString(R.string.error_map, G.app.name);
        return builder(c, msg).setPositiveButton(R.string.action_ok, (d, w) -> c.finish()).create();
    }

    private static AlertDialog.Builder builder(Context c, int msgId) {
        return builder(c, c.getString(msgId));
    }

    private static AlertDialog.Builder builder(Context c, String msg) {
        return new AlertDialog.Builder(c).setTitle(R.string.title_error).setMessage(msg);
    }
}
