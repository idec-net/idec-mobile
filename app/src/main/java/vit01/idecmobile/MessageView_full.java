package vit01.idecmobile;

import android.content.Context;
import android.net.Uri;
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

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MessageView_full.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MessageView_full#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MessageView_full extends Fragment {
    private AbstractTransport transport;
    private ArrayList<String> msglist;
    private int position;

    private OnFragmentInteractionListener mListener;

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
            transport = new SqliteTransport(getActivity());
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

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
