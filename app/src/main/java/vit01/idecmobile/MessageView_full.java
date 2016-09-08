package vit01.idecmobile;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.SqliteTransport;

public class MessageView_full extends Fragment {
    private AbstractTransport transport;
    private ArrayList<String> msglist;
    private int position;

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

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootLayout = inflater.inflate(R.layout.message_view, null, false);

        String msgid = msglist.get(position);
        IIMessage message = transport.getMessage(msgid);
        if (message == null) message = new IIMessage();

        TextView full_subj, full_msg, full_from_to, full_date, full_msgid, full_repto;

        full_subj = (TextView) rootLayout.findViewById(R.id.full_subj);
        full_msg = (TextView) rootLayout.findViewById(R.id.full_text);
        full_from_to = (TextView) rootLayout.findViewById(R.id.full_from_to);
        full_date = (TextView) rootLayout.findViewById(R.id.full_date);
        full_msgid = (TextView) rootLayout.findViewById(R.id.full_msgid);
        full_repto = (TextView) rootLayout.findViewById(R.id.full_repto);

        full_subj.setText(message.subj);
        full_msg.setText(message.msg);
        full_from_to.setText(message.from + " (" + message.addr + ") to " + message.to);
        full_msgid.setText("msgid: " + msgid);
        String repto_insert = (message.repto != null) ? message.repto : "-";
        full_repto.setText("Ответ: " + repto_insert);
        full_date.setText(SimpleFunctions.timestamp2date(message.time, true));

        return rootLayout;
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
