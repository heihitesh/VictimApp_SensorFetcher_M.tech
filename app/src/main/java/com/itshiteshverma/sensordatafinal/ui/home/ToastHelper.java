package com.itshiteshverma.sensordatafinal.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import com.itshiteshverma.sensordatafinal.R;

public class ToastHelper {
    /* access modifiers changed from: private */
    Context mContext;
    LayoutInflater inflater;

    public ToastHelper(Context mContext, LayoutInflater inflater) {
        this.mContext = mContext;
        this.inflater = inflater;
    }

//    public void toastFloatingDark(String msg) {
//        Toast toast = new Toast(mContext);
//        toast.setDuration(Toast.LENGTH_LONG);
//        View inflate = inflater.inflate(R.layout.snackbar_toast_floating_dark, null);
//        inflate.findViewById(R.id.tv_undo).setVisibility(View.VISIBLE);
//        inflate.findViewById(R.id.separator).setVisibility(View.VISIBLE);
//        toast.setView(inflate);
//        toast.show();
//    }

    /* access modifiers changed from: private */
    public void toastIconError(String msg) {
        Toast toast = new Toast(mContext);
        toast.setDuration(Toast.LENGTH_LONG);
        View inflate = inflater.inflate(R.layout.toast_icon_text, null);
        ((TextView) inflate.findViewById(R.id.message)).setText("Error " + msg);
        ((ImageView) inflate.findViewById(R.id.icon)).setImageResource(R.drawable.ic_close);
        ((CardView) inflate.findViewById(R.id.parent_view)).setCardBackgroundColor(mContext.getResources().getColor(R.color.red_600));
        toast.setView(inflate);
        toast.show();
    }

    /* access modifiers changed from: private */
    public void toastIconSuccess(String msg) {
        Toast toast = new Toast(mContext);
        toast.setDuration(Toast.LENGTH_LONG);
        View inflate = inflater.inflate(R.layout.toast_icon_text, null);
        ((TextView) inflate.findViewById(R.id.message)).setText("Success " + msg);
        ((ImageView) inflate.findViewById(R.id.icon)).setImageResource(R.drawable.ic_done_black_24dp);
        ((CardView) inflate.findViewById(R.id.parent_view)).setCardBackgroundColor(mContext.getResources().getColor(R.color.green_500));
        toast.setView(inflate);
        toast.show();
    }

    /* access modifiers changed from: private */
    public void toastIconInfo(String msg) {
        Toast toast = new Toast(mContext);
        toast.setDuration(Toast.LENGTH_LONG);
        View inflate = inflater.inflate(R.layout.toast_icon_text, null);
        ((TextView) inflate.findViewById(R.id.message)).setText(msg);
        ((ImageView) inflate.findViewById(R.id.icon)).setImageResource(R.drawable.ic_error_outline);
        ((CardView) inflate.findViewById(R.id.parent_view)).setCardBackgroundColor(mContext.getResources().getColor(R.color.blue_500));
        toast.setView(inflate);
        toast.show();
    }
}
