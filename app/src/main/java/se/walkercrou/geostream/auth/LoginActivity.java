package se.walkercrou.geostream.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import se.walkercrou.geostream.App;
import se.walkercrou.geostream.MapActivity;
import se.walkercrou.geostream.R;
import se.walkercrou.geostream.net.request.ApiRequest;
import se.walkercrou.geostream.net.request.Request;
import se.walkercrou.geostream.net.response.ApiResponse;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends Activity {
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private UserLoginTask authTask = null;

    // UI references.
    private EditText usernameView;
    private EditText passwordView;
    private View progressView;
    private View loginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        usernameView = (EditText) findViewById(R.id.username);

        passwordView = (EditText) findViewById(R.id.password);
        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button signInButton = (Button) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        loginFormView = findViewById(R.id.signup_form);
        progressView = findViewById(R.id.signup_progress);
    }

    public void attemptLogin() {
        App.d("Attempting login");
        if (authTask != null) {
            return;
        }

        // Reset errors.
        usernameView.setError(null);
        passwordView.setError(null);

        // Store values at the time of the login attempt.
        String email = usernameView.getText().toString();
        String password = passwordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            passwordView.setError(getString(R.string.error_invalid_password));
            focusView = passwordView;
            cancel = true;
        }

        // Check for a valid username address.
        if (TextUtils.isEmpty(email)) {
            usernameView.setError(getString(R.string.error_field_required));
            focusView = usernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            authTask = new UserLoginTask(email, password);
            authTask.execute((Void) null);
        }
    }

    private boolean isPasswordValid(String password) {
        int len = password.length();
        return len >= MIN_PASSWORD_LENGTH && len <= MAX_PASSWORD_LENGTH;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            loginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public void openSignupForm(View view) {
        // called when the "Sign up" button is clicked
        startActivity(new Intent(this, SignupActivity.class));
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        private final String username;
        private final String password;
        private String error;
        private boolean badPassword = false;

        public UserLoginTask(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            App.d("Attempting login for : \"" + username + "\"");

            // check if the user exists
            ApiResponse response = new ApiRequest(
                    Request.METHOD_GET, ApiRequest.URL_USER_DETAIL, username
            ).setAuthorization(username, password).send();

            // check response for error
            if (response == null)
                return noConnection();
            else if (response.isError()) {
                if (response.getStatusCode() == Request.STATUS_UNAUTHORIZED) {
                    // bad username or password, or the user does not exist
                    badPassword = true;
                    return false;
                }
                // some other error
                return error(response.getErrorDetail());
            }

            // success, get user id
            int userId;
            try {
                userId = ((JSONObject) response.get()).getInt("id");
            } catch (JSONException e) {
                App.e("An error occurred while reading the JSON response from the server", e);
                return false;
            }

            // commit user info to preferences
            App.getSharedPreferences(LoginActivity.this).edit()
                    .putInt(App.PREF_USER_ID, userId)
                    .putString(App.PREF_USER_NAME, username)
                    .putString(App.PREF_USER_PASSWORD, password)
                    .commit();

            return true;
        }

        private boolean noConnection() {
            error = LoginActivity.this.getString(R.string.error_no_connection);
            error = String.format(error, App.getName());
            return false;
        }

        private boolean error(String error) {
            this.error = error;
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            authTask = null;
            showProgress(false);

            if (success) {
                startActivity(new Intent(LoginActivity.this, MapActivity.class));
            } else if (badPassword){
                passwordView.setError(getString(R.string.error_incorrect_password));
                passwordView.requestFocus();
            } else {
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            authTask = null;
            showProgress(false);
        }
    }
}

