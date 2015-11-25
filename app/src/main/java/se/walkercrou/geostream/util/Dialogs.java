package se.walkercrou.geostream.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;

import se.walkercrou.geostream.R;

import static android.content.DialogInterface.OnClickListener;

/**
 * Helper class for building dialogs to display.
 */
public final class Dialogs {
    private Dialogs() {
    }

    /**
     * Returns a connection error {@link AlertDialog}.
     *
     * @param c              context
     * @param tryAgainAction button action
     * @return alert dialog
     */
    public static AlertDialog connectionError(Context c, OnClickListener tryAgainAction) {
        String msg = c.getString(R.string.error_no_connection, G.app.name);
        return errorBuilder(c, msg)
                .setPositiveButton(R.string.action_try_again, tryAgainAction)
                .create();
    }

    /**
     * Returns a dialog to display when an error occurs while attempting to send a post.
     *
     * @param c context
     * @return alert dialog
     */
    public static AlertDialog sendPostError(Context c) {
        return errorBuilder(c, R.string.error_send_post)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                })
                .create();
    }

    /**
     * Returns a dialog to display when an error occurs while opening the camera.
     *
     * @param c context
     * @return alert dialog
     */
    public static AlertDialog openCameraError(final Activity c) {
        String msg = c.getString(R.string.error_no_camera, G.app.name);
        return errorBuilder(c, msg)
                .setPositiveButton(R.string.action_back, (dialog, which) -> c.finish())
                .create();
    }

    /**
     * Returns a dialog to display when an error occurs while trying to get the device's location.
     *
     * @param c              context
     * @param tryAgainAction action if user presses try again
     * @return alert dialog
     */
    public static AlertDialog locationError(Context c, OnClickListener tryAgainAction) {
        String msg = c.getString(R.string.error_no_location, G.app.name);
        return errorBuilder(c, msg)
                .setPositiveButton(R.string.action_try_again, tryAgainAction)
                .create();
    }

    private static AlertDialog.Builder errorBuilder(Context c, int msgId) {
        return errorBuilder(c, c.getString(msgId));
    }

    private static AlertDialog.Builder errorBuilder(Context c, String msg) {
        return new AlertDialog.Builder(c).setTitle(R.string.error_title).setMessage(msg);
    }
}
