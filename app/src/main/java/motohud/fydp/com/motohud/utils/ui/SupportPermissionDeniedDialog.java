package motohud.fydp.com.motohud.utils.ui;

/**
 * Created by Shing on 2018-03-14.
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

import motohud.fydp.com.motohud.R;
import motohud.fydp.com.motohud.utils.PermissionUtils;

/**
 * A dialog that displays a permission denied message.
 */
public class SupportPermissionDeniedDialog extends DialogFragment {

    private static final String ARGUMENT_FINISH_ACTIVITY = "finish";

    private boolean mFinishActivity = false;

    /**
     * Creates a new instance of this dialog and optionally finishes the calling Activity
     * when the 'Ok' button is clicked.
     */
    public static SupportPermissionDeniedDialog newInstance(boolean finishActivity) {
        Bundle arguments = new Bundle();
        arguments.putBoolean(ARGUMENT_FINISH_ACTIVITY, finishActivity);

        SupportPermissionDeniedDialog dialog = new SupportPermissionDeniedDialog();
        dialog.setArguments(arguments);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mFinishActivity = getArguments().getBoolean(ARGUMENT_FINISH_ACTIVITY);

        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.location_permission_denied)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mFinishActivity) {
            Toast.makeText(getActivity(), R.string.permission_required_toast,
                    Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
    }
}