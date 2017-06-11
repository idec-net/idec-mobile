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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import vit01.idecmobile.Core.EchoReadingPosition;
import vit01.idecmobile.Core.ExternalStorage;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.prefs.Config;

public class MessageSlideFragment extends Fragment {
    ActionBar actionBar;
    ViewPager mPager;
    boolean stackUpdate = false;
    private int msgCount;
    private int nodeIndex;
    private ArrayList<String> msglist;
    private ArrayList<Integer> discussionStack = new ArrayList<>();
    private String echoarea = null;

    public MessageSlideFragment() {
    }

    public static MessageSlideFragment newInstance(

    ) {
        MessageSlideFragment fragment = new MessageSlideFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_message_slide, container, false);
        mPager = (ViewPager) rootView.findViewById(R.id.swipe_pager);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateActionBar(position);
                if (Config.values.disableMsglist && IDECFunctions.isRealEchoarea(echoarea))
                    EchoReadingPosition.setPosition(echoarea, msglist.get(position));

                if ((discussionStack.size() > 0) && discussionStack.get(0).equals(position) && !stackUpdate) {
                    discussionStack.remove(0);
                } else {
                    if (!stackUpdate) discussionStack.clear();
                    else stackUpdate = false;
                }

                // Помечаем сообщение прочитанным
                final String msgid = msglist.get(position);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        GlobalTransport.transport.setUnread(false, Collections.singletonList(msgid));
                    }
                }).start();
            }
        });
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    public void updateActionBar(int position) {
        // actionBar.setTitle(String.valueOf(position + 1) + " из " + msgCount);
    }

    public void initSlider(String echo, ArrayList<String> msgids, int nIndex, int firstPosition) {
        SimpleFunctions.resetIDECParserColors();
        msglist = msgids;

        if (msglist == null || msglist.size() == 0) {
            Toast.makeText(getActivity(), "Список сообщений пуст, выходим", Toast.LENGTH_SHORT).show();
            return;
        }

        nodeIndex = nIndex < 0 ? Config.currentSelectedStation : nIndex;

        msgCount = msglist.size();
        echoarea = echo;

        PagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getFragmentManager());
        mPager.setAdapter(pagerAdapter);
        mPager.setCurrentItem(firstPosition);

        // помечаем прочитанным первое сообщение
        GlobalTransport.transport.setUnread(false, Collections.singletonList(msglist.get(firstPosition)));
        updateActionBar(firstPosition);
        if (Config.values.disableMsglist && IDECFunctions.isRealEchoarea(echoarea)) {
            EchoReadingPosition.setPosition(echoarea, msglist.get(firstPosition));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Activity activity = getActivity();
        inflater.inflate(R.menu.message_view, menu);
        MenuItem return_msglist = menu.findItem(R.id.action_msglist_return);

        int iconColor = SimpleFunctions.colorFromTheme(activity, R.attr.menuIconColor);

        if (!SimpleFunctions.isTablet(activity) && Config.values.disableMsglist && echoarea != null) {
            return_msglist.setVisible(true);
            return_msglist.setIcon(new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_format_list_bulleted).actionBar().color(iconColor));
        }

        MenuItem tostart = menu.findItem(R.id.action_first_item);
        MenuItem toend = menu.findItem(R.id.action_last_item);

        if (msgCount == 1) {
            tostart.setVisible(false);
            toend.setVisible(false);
        } else {
            IconicsDrawable startIcon = new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_first_page).actionBar().color(iconColor);
            IconicsDrawable endIcon = new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_last_page).actionBar().color(iconColor);

            IconicsDrawable discussionNextIcon = new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_chevron_right).actionBar().color(iconColor);

            tostart.setIcon(startIcon);
            toend.setIcon(endIcon);

            menu.findItem(R.id.action_discussion_previous).setVisible(false);
            menu.findItem(R.id.action_discussion_next).setVisible(true).setIcon(discussionNextIcon);
        }

        IconicsDrawable newMsg = new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_create).actionBar().color(iconColor);
        MenuItem compose = menu.findItem(R.id.action_new_message);
        compose.setIcon(newMsg);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Activity callingActivity = getActivity();

        switch (item.getItemId()) {
            case R.id.action_first_item:
                mPager.setCurrentItem(0, false);
                return true;
            case R.id.action_last_item:
                mPager.setCurrentItem(msgCount - 1, false);
                return true;
            case R.id.action_new_message:
                IIMessage msg = GlobalTransport.transport.getMessage
                        (msglist.get(mPager.getCurrentItem()));

                Intent intent = new Intent(callingActivity, DraftEditor.class);
                intent.putExtra("task", "new_in_echo");
                intent.putExtra("echoarea", msg.echo);
                intent.putExtra("nodeindex", nodeIndex);
                startActivity(intent);
                return true;
            case R.id.action_msglist_return:
                EchoReadingPosition.writePositionCache();
                callingActivity.setResult(0);
                callingActivity.finish();
                return true;
            case R.id.action_discussion_previous:
                int pos = mPager.getCurrentItem();
                String repto = GlobalTransport.transport
                        .getMessage(msglist.get(pos)).tags.get("repto");

                if (repto == null) {
                    Toast.makeText(callingActivity, "Этот пользователь никому не отвечал!", Toast.LENGTH_SHORT).show();
                    break;
                } else {
                    if (msglist.contains(repto)) {
                        int newindex = msglist.indexOf(repto);
                        discussionStack.add(0, pos);
                        stackUpdate = true;
                        mPager.setCurrentItem(newindex);
                    } else {
                        Toast.makeText(callingActivity, "В данном списке сообщений нет того, на которое отвечали.\nМожет быть, надо сначала зайти в саму эху?", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.action_discussion_next:
                if (discussionStack.size() > 0) {
                    stackUpdate = true;
                    mPager.setCurrentItem(discussionStack.remove(0));
                } else
                    Toast.makeText(callingActivity, "Стек дискуссии пуст!", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_save_in_file:
                String msgid = msglist.get(mPager.getCurrentItem());
                String msgRaw = GlobalTransport.transport.getMessage(msgid).raw();
                File file = new File(ExternalStorage.rootStorage.getParentFile(), msgid + ".txt");

                if (!file.exists()) try {
                    boolean create = file.createNewFile();

                    if (!create) {
                        String debug = "Не могу создать файл " + file.getName();
                        Toast.makeText(callingActivity, debug, Toast.LENGTH_SHORT).show();
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    SimpleFunctions.debug(e.getMessage());
                    break;
                }
                if (file.canWrite()) {
                    try {
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(msgRaw.getBytes("UTF-8"));
                        fos.close();
                    } catch (Exception e) {
                        SimpleFunctions.debug(e.getMessage());
                        Toast.makeText(callingActivity, "Ошибка: " +
                                e.getMessage(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                        break;
                    }

                    new AlertDialog.Builder(callingActivity)
                            .setMessage("Сообщение сохранено в файл " + file.getAbsolutePath())
                            .setPositiveButton("Ясно", null)
                            .show();
                } else {
                    Toast.makeText(callingActivity, "Файл " + file.getAbsolutePath() + " недоступен для записи.", Toast.LENGTH_SHORT).show();
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        if ((Config.values.disableMsglist || SimpleFunctions.isTablet(getActivity()))
                && IDECFunctions.isRealEchoarea(echoarea))
            EchoReadingPosition.writePositionCache();
        super.onPause();
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return MessageView_full.newInstance(msglist.get(position));
        }

        @Override
        public int getCount() {
            return msgCount;
        }
    }
}
