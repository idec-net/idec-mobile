package vit01.idecmobile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.util.ArrayList;
import java.util.Arrays;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.SqliteTransport;

public class MessageView_full extends Fragment {
    private AbstractTransport transport;
    private ArrayList<String> msglist;
    private int position;
    private String msgid;

    private boolean messageStarred = false;

    private Context mListener;

    public MessageView_full() {
        // Required empty public constructor
    }

    public static MessageView_full newInstance(
            ArrayList<String> msglist,
            int position
    ) {
        MessageView_full fragment = new MessageView_full();
        Bundle args = new Bundle();
        args.putStringArrayList("msglist", msglist);
        args.putInt("position", position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            transport = new SqliteTransport(mListener);
            msglist = getArguments().getStringArrayList("msglist");
            position = getArguments().getInt("position");
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootLayout = inflater.inflate(R.layout.message_view, null, false);

        msgid = msglist.get(position);
        IIMessage message = transport.getMessage(msgid);
        if (message == null) message = new IIMessage();

        TextView full_subj, full_msg, full_from_to, full_date, full_msgid, full_repto, full_echo;

        full_subj = (TextView) rootLayout.findViewById(R.id.full_subj);
        full_msg = (TextView) rootLayout.findViewById(R.id.full_text);
        full_from_to = (TextView) rootLayout.findViewById(R.id.full_from_to);
        full_date = (TextView) rootLayout.findViewById(R.id.full_date);
        full_msgid = (TextView) rootLayout.findViewById(R.id.full_msgid);
        full_repto = (TextView) rootLayout.findViewById(R.id.full_repto);
        full_echo = (TextView) rootLayout.findViewById(R.id.full_echo);

        messageStarred = message.is_favorite;
        full_subj.setText(message.subj);
        full_msg.setText(Html.fromHtml(SimpleFunctions.reparseMessage(message.msg)));
        full_from_to.setText(message.from + " (" + message.addr + ") to " + message.to);
        full_msgid.setText("msgid: " + msgid);
        String repto_insert = (message.repto != null) ? message.repto : "-";
        full_repto.setText("Ответ: " + repto_insert);
        full_date.setText(SimpleFunctions.timestamp2date(message.time, true));
        full_echo.setText(message.echo);

        Button fullAnswerBtn = (Button) rootLayout.findViewById(R.id.full_answer_button);
        fullAnswerBtn.setCompoundDrawablesWithIntrinsicBounds(null, new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_reply).sizeDp(24).color(Color.GRAY), null, null);
        fullAnswerBtn.setCompoundDrawablePadding(30);
        fullAnswerBtn.setTextColor(Color.GRAY);
        Button fullQuoteAnswerBtn = (Button) rootLayout.findViewById(R.id.full_quote_answer_button);
        fullQuoteAnswerBtn.setCompoundDrawablesWithIntrinsicBounds(null, new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_format_quote).sizeDp(24).color(Color.GRAY), null, null);
        fullQuoteAnswerBtn.setCompoundDrawablePadding(30);
        fullQuoteAnswerBtn.setTextColor(Color.GRAY);

        return rootLayout;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.message_view, menu);

        MenuItem starredItem = menu.findItem(R.id.action_starred);
        if (messageStarred) {
            setStarredIcon(true, starredItem);
        } else {
            setStarredIcon(false, starredItem);
        }

        super.onCreateOptionsMenu(menu, inflater);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_starred:
                messageStarred = !messageStarred;
                setStarredIcon(messageStarred, item);
                transport.setFavorite(messageStarred, Arrays.asList(msgid));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setStarredIcon(boolean isStarred, MenuItem item) {
        Drawable icon;
        if (isStarred) {
            icon = new IconicsDrawable(getContext(), GoogleMaterial.Icon.gmd_star)
                    .actionBar().color(Color.WHITE);
        } else {
            icon = new IconicsDrawable(getContext(), GoogleMaterial.Icon.gmd_star)
                    .actionBar().color(Color.WHITE).alpha(80);
        }
        item.setIcon(icon);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
