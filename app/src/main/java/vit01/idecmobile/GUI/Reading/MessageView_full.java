/*
 * Copyright (c) 2016-2017 Viktor Fedenyov <me@ii-net.tk> <https://ii-net.tk>
 *
 * This file is part of IDEC Mobile.
 *
 * IDEC Mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IDEC Mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IDEC Mobile.  If not, see <http://www.gnu.org/licenses/>.
 */

package vit01.idecmobile.GUI.Reading;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.GUI.Drafts.DraftEditor;
import vit01.idecmobile.QuoteEditActivity;
import vit01.idecmobile.R;
import vit01.idecmobile.gui_helpers.CustomLinkMovementMethod;
import vit01.idecmobile.gui_helpers.MyTextView;

public class MessageView_full extends Fragment {
    public AbstractTransport transport;
    public boolean messageStarred = false;
    MenuItem discussionBack;
    TextView full_subj, full_from_to, full_date, full_msgid, full_repto, full_echo;
    MyTextView full_msg;
    Fragment parentContext;
    Button fullNewMessageBtn;
    private String msgid;
    private IIMessage message;

    public MessageView_full() {
        // Required empty public constructor
    }

    public static MessageView_full newInstance(String msgid) {
        MessageView_full fragment = new MessageView_full();
        Bundle args = new Bundle();
        args.putString("msgid", msgid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            msgid = getArguments().getString("msgid");
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootLayout = inflater.inflate(R.layout.message_view, null, false);

        full_subj = rootLayout.findViewById(R.id.full_subj);
        full_msg = rootLayout.findViewById(R.id.full_text);
        full_from_to = rootLayout.findViewById(R.id.full_from_to);
        full_date = rootLayout.findViewById(R.id.full_date);
        full_msgid = rootLayout.findViewById(R.id.full_msgid);
        full_repto = rootLayout.findViewById(R.id.full_repto);
        full_echo = rootLayout.findViewById(R.id.full_echo);

        full_msgid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager)
                        getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("idec msgid", message.id);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(getActivity(), R.string.msgid_clipboard_done, Toast.LENGTH_SHORT).show();
            }
        });
        full_msg.setMovementMethod(CustomLinkMovementMethod.getInstance());

        int secondaryColor = SimpleFunctions.colorFromTheme(getActivity(), android.R.attr.textColorSecondary);

        Button fullAnswerBtn = rootLayout.findViewById(R.id.full_answer_button);
        fullAnswerBtn.setCompoundDrawablesWithIntrinsicBounds(null, new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_reply).sizeDp(20).color(secondaryColor), null, null);
        fullAnswerBtn.setCompoundDrawablePadding(30);

        fullAnswerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), DraftEditor.class);
                intent.putExtra("task", "new_answer");
                intent.putExtra("nodeindex",
                        SimpleFunctions.getPreferredOutboxId(message.echo));
                intent.putExtra("message", message);
                intent.putExtra("quote", false);

                startActivity(intent);
            }
        });

        Button fullQuoteAnswerBtn = rootLayout.findViewById(R.id.full_quote_answer_button);
        fullQuoteAnswerBtn.setCompoundDrawablesWithIntrinsicBounds(null, new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_format_quote).sizeDp(20).color(secondaryColor), null, null);
        fullQuoteAnswerBtn.setCompoundDrawablePadding(30);

        fullQuoteAnswerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), DraftEditor.class);
                intent.putExtra("task", "new_answer");
                intent.putExtra("nodeindex",
                        SimpleFunctions.getPreferredOutboxId(message.echo));
                intent.putExtra("message", message);
                intent.putExtra("quote", true);

                startActivity(intent);
            }
        });

        fullQuoteAnswerBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(getActivity(), QuoteEditActivity.class);
                intent.putExtra("nodeindex",
                        SimpleFunctions.getPreferredOutboxId(message.echo));
                intent.putExtra("message", message);

                startActivity(intent);
                return true;
            }
        });

        fullNewMessageBtn = rootLayout.findViewById(R.id.full_new_button);
        fullNewMessageBtn.setCompoundDrawablesWithIntrinsicBounds(null, new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_create).sizeDp(20).color(secondaryColor), null, null);
        fullNewMessageBtn.setCompoundDrawablePadding(30);

        fullNewMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), DraftEditor.class);
                intent.putExtra("task", "new_in_echo");
                intent.putExtra("echoarea", message.echo);
                intent.putExtra("nodeindex", SimpleFunctions.getPreferredOutboxId(message.echo));
                startActivity(intent);
            }
        });

        initializeMessage(getContext());
        return rootLayout;
    }

    public void initializeMessage(Context context) {
        message = GlobalTransport.transport(context).getMessage(msgid);
        if (message == null) message = new IIMessage();

        messageStarred = message.is_favorite;
        full_subj.setText(message.subj);
        full_msg.setText(Html.fromHtml(SimpleFunctions.reparseMessage(context, message.msg)));
        full_from_to.setText(String.format("%s (%s) to %s", message.from, message.addr, message.to));
        full_msgid.setText(String.format("msgid: %s", msgid));

        if (message.repto == null) {
            full_repto.setVisibility(View.GONE);
        } else {
            full_repto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (parentContext != null) parentContext.onOptionsItemSelected(discussionBack);
                }
            });
            full_repto.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ClipboardManager clipboard = (ClipboardManager)
                            getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("idec msgid", message.repto);
                    clipboard.setPrimaryClip(clip);

                    Toast.makeText(getActivity(), R.string.repto_clipboard_done, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            full_repto.setText(getString(R.string.answered, message.repto));
        }

        if (message.echo == null || message.echo.equals("") || message.echo.equals("no.echo")) {
            fullNewMessageBtn.setVisibility(View.GONE);
        }

        full_date.setText(SimpleFunctions.timestamp2date(message.time));
        full_echo.setText(message.echo);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        discussionBack = menu.findItem(R.id.action_discussion_previous);

        super.onCreateOptionsMenu(menu, inflater);
    }
}
