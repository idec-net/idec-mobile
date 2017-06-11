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

package vit01.idecmobile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.util.Collections;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.gui_helpers.CustomLinkMovementMethod;

public class MessageView_full extends Fragment {
    public AbstractTransport transport = GlobalTransport.transport;
    MenuItem discussionBack;
    private String msgid;
    private IIMessage message;
    private boolean messageStarred = false;

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
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootLayout = inflater.inflate(R.layout.message_view, null, false);

        message = transport.getMessage(msgid);
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
        full_msg.setText(Html.fromHtml(SimpleFunctions.reparseMessage(inflater.getContext(), message.msg)));
        full_from_to.setText(message.from + " (" + message.addr + ") to " + message.to);
        full_msgid.setText("msgid: " + msgid);

        full_msgid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager)
                        getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("idec msgid", message.id);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(getActivity(), "id сообщения скопирован в буфер обмена", Toast.LENGTH_SHORT).show();
            }
        });

        if (message.repto == null) {
            full_repto.setVisibility(View.GONE);
        } else {
            full_repto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivity().onOptionsItemSelected(discussionBack);
                }
            });
            full_repto.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ClipboardManager clipboard = (ClipboardManager)
                            getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("idec msgid", message.repto);
                    clipboard.setPrimaryClip(clip);

                    Toast.makeText(getActivity(), "msgid ответа скопирован в буфер обмена", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            full_repto.setText("Ответ: " + message.repto);
        }

        full_date.setText(SimpleFunctions.timestamp2date(message.time));
        full_echo.setText(message.echo);

        full_msg.setMovementMethod(CustomLinkMovementMethod.getInstance());

        Button fullAnswerBtn = (Button) rootLayout.findViewById(R.id.full_answer_button);

        int secondaryColor = SimpleFunctions.colorFromTheme(getActivity(), android.R.attr.textColorSecondary);

        fullAnswerBtn.setCompoundDrawablesWithIntrinsicBounds(null, new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_reply).sizeDp(24).color(secondaryColor), null, null);
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

        Button fullQuoteAnswerBtn = (Button) rootLayout.findViewById(R.id.full_quote_answer_button);
        fullQuoteAnswerBtn.setCompoundDrawablesWithIntrinsicBounds(null, new IconicsDrawable(getActivity(), GoogleMaterial.Icon.gmd_format_quote).sizeDp(24).color(secondaryColor), null, null);
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
        return rootLayout;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem starredItem = menu.findItem(R.id.action_starred);
        discussionBack = menu.findItem(R.id.action_discussion_previous);
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
                transport.setFavorite(messageStarred, Collections.singletonList(msgid));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setStarredIcon(boolean isStarred, MenuItem item) {
        Drawable icon;
        Context context = getContext();

        int iconColor = SimpleFunctions.colorFromTheme(context, R.attr.menuIconColor);

        if (isStarred) {
            icon = new IconicsDrawable(context, GoogleMaterial.Icon.gmd_star)
                    .actionBar().color(iconColor);
        } else {
            icon = new IconicsDrawable(context, GoogleMaterial.Icon.gmd_star)
                    .actionBar().color(iconColor).alpha(80);
        }
        item.setIcon(icon);
    }
}
