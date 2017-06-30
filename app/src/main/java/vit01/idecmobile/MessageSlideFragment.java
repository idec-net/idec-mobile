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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
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
import vit01.idecmobile.Core.Fetcher;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.IDECFunctions;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.prefs.Config;

public class MessageSlideFragment extends Fragment {
    public boolean isTablet, isRealEchoarea;
    Activity activity;
    ViewPager mPager;
    Drawable starredIcon, unstarredIcon;
    MenuItem starredMenuItem;
    MessageListFragment listFragment = null;
    boolean stackUpdate = false;
    private int msgCount;
    private int nodeIndex;
    private ArrayList<String> msglist;
    private ArrayList<Integer> discussionStack = new ArrayList<>();
    private String echoarea = null;
    private String appendToTitle = null; // Здесь должно быть имя эхи и разделитель |

    public MessageSlideFragment() {
    }

    public static MessageSlideFragment newInstance() {
        return new MessageSlideFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_message_slide, container, false);
        mPager = (ViewPager) rootView.findViewById(R.id.swipe_pager);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(final int position) {
                updateActionBar(position);
                if (isRealEchoarea)
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
                        if (listFragment != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listFragment.mAdapter.messageChanged(msgid);
                                }
                            });
                        }
                    }
                }).start();
            }
        });
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        listFragment = (MessageListFragment) getFragmentManager().findFragmentById(R.id.msglist);
        setHasOptionsMenu(true);
    }

    public void updateActionBar(int position) {
        SimpleFunctions.setActivityTitle((AppCompatActivity) activity,
                appendToTitle + String.valueOf(position + 1) + " из " + msgCount);
    }

    public void initSlider(String echo, ArrayList<String> msgids, int nIndex, int firstPosition) {
        SimpleFunctions.resetIDECParserColors();
        msglist = msgids;

        if (msglist == null || msglist.size() == 0) {
            Toast.makeText(activity, "Список сообщений пуст...", Toast.LENGTH_SHORT).show();
            return;
        }

        nodeIndex = nIndex < 0 ? Config.currentSelectedStation : nIndex;

        msgCount = msglist.size();
        echoarea = echo;

        isRealEchoarea = IDECFunctions.isRealEchoarea(echoarea);
        isTablet = SimpleFunctions.isTablet(activity);

        if (isTablet) { // Если на планшете, то приписываем к счётчику сверху имя эхи
            String prettyName = IDECFunctions.getAreaName(echoarea);
            if (prettyName.equals("")) appendToTitle = "";
            else appendToTitle = prettyName + ",  ";
        } else appendToTitle = "";

        PagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getFragmentManager());
        mPager.setAdapter(pagerAdapter);
        mPager.setCurrentItem(firstPosition);

        // помечаем прочитанным первое сообщение
        GlobalTransport.transport.setUnread(false, Collections.singletonList(msglist.get(firstPosition)));
        updateActionBar(firstPosition);

        if (isRealEchoarea) {
            EchoReadingPosition.setPosition(echoarea, msglist.get(firstPosition));
        }
    }

    public void setStarredIcon(boolean isStarred, MenuItem item) {
        if (!item.isVisible()) item.setVisible(true);

        item.setIcon(isStarred ? starredIcon : unstarredIcon);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.message_view, menu);
        MenuItem return_msglist = menu.findItem(R.id.action_msglist_return);

        int iconColor = SimpleFunctions.colorFromTheme(activity, R.attr.menuIconColor);

        if (!isTablet && Config.values.disableMsglist && echoarea != null) {
            return_msglist.setVisible(true);
            return_msglist.setIcon(new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_format_list_bulleted).actionBar().color(iconColor));
        }

        MenuItem tostart = menu.findItem(R.id.action_first_item);
        MenuItem toend = menu.findItem(R.id.action_last_item);

        starredMenuItem = menu.findItem(R.id.action_starred);
        starredIcon = new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_star)
                .actionBar().color(iconColor);
        unstarredIcon = new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_star)
                .actionBar().color(iconColor).alpha(80);

        if (mPager != null) {
            ScreenSlidePagerAdapter adapter = (ScreenSlidePagerAdapter) mPager.getAdapter();
            if (adapter != null) {
                MessageView_full fragm = adapter.mCurrentFragment;
                if (fragm != null) setStarredIcon(fragm.messageStarred, starredMenuItem);
            }
        }

        if (msgCount <= 1) {
            tostart.setVisible(false);
            toend.setVisible(false);

            if (msgCount == 0) menu.findItem(R.id.action_save_in_file).setVisible(false);
        } else {
            IconicsDrawable startIcon = new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_first_page).actionBar().color(iconColor);
            IconicsDrawable endIcon = new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_last_page).actionBar().color(iconColor);

            IconicsDrawable discussionNextIcon = new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_chevron_right).actionBar().color(iconColor);

            tostart.setIcon(startIcon);
            toend.setIcon(endIcon);

            // menu.findItem(R.id.action_discussion_previous).setVisible(false);
            menu.findItem(R.id.action_discussion_next).setVisible(true).setIcon(discussionNextIcon);
        }

        MenuItem compose = menu.findItem(R.id.action_new_message);

        if (!isTablet && isRealEchoarea) {
            IconicsDrawable newMsg = new IconicsDrawable(activity, GoogleMaterial.Icon.gmd_create).actionBar().color(iconColor);
            compose.setIcon(newMsg);
        } else compose.setVisible(false);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_first_item:
                mPager.setCurrentItem(0, false);
                return true;
            case R.id.action_last_item:
                mPager.setCurrentItem(msgCount - 1, false);
                return true;
            case R.id.action_starred:
                MessageView_full current_fragment = ((ScreenSlidePagerAdapter) mPager.getAdapter()).mCurrentFragment;
                String msgid = msglist.get(mPager.getCurrentItem());
                current_fragment.messageStarred = !current_fragment.messageStarred;
                GlobalTransport.transport.setFavorite(
                        current_fragment.messageStarred, Collections.singletonList(msgid));
                activity.invalidateOptionsMenu();

                if (listFragment != null) listFragment.mAdapter.messageChanged(msgid, false);
                return true;
            case R.id.action_new_message:
                IIMessage msg = GlobalTransport.transport.getMessage
                        (msglist.get(mPager.getCurrentItem()));

                Intent intent = new Intent(activity, DraftEditor.class);
                intent.putExtra("task", "new_in_echo");
                intent.putExtra("echoarea", msg.echo);
                intent.putExtra("nodeindex", nodeIndex);
                startActivity(intent);
                return true;
            case R.id.action_msglist_return:
                onPause();
                activity.setResult(0);
                activity.finish();
                return true;
            case R.id.action_discussion_previous:
                int pos = mPager.getCurrentItem();
                String repto = GlobalTransport.transport
                        .getMessage(msglist.get(pos)).tags.get("repto");

                if (repto == null) {
                    Toast.makeText(activity, "Этот пользователь никому не отвечал!", Toast.LENGTH_SHORT).show();
                    break;
                } else {
                    if (msglist.contains(repto)) {
                        int newindex = msglist.indexOf(repto);
                        discussionStack.add(0, pos);
                        stackUpdate = true;
                        mPager.setCurrentItem(newindex);
                    } else {
                        Toast.makeText(activity, "В данном списке сообщений нет того, на которое отвечали.\nМожет быть, надо сначала зайти в саму эху?", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.action_discussion_next:
                if (discussionStack.size() > 0) {
                    stackUpdate = true;
                    mPager.setCurrentItem(discussionStack.remove(0));
                } else
                    Toast.makeText(activity, "Стек дискуссии пуст!", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_save_in_file:
                String _msgid = msglist.get(mPager.getCurrentItem());
                String msgRaw = GlobalTransport.transport.getMessage(_msgid).raw();
                File file = new File(ExternalStorage.rootStorage.getParentFile(), _msgid + ".txt");

                if (!file.exists()) try {
                    boolean create = file.createNewFile();

                    if (!create) {
                        String debug = "Не могу создать файл " + file.getName();
                        Toast.makeText(activity, debug, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(activity, "Ошибка: " +
                                e.getMessage(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                        break;
                    }

                    new AlertDialog.Builder(activity)
                            .setMessage("Сообщение сохранено в файл " + file.getAbsolutePath())
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                } else {
                    Toast.makeText(activity, "Файл " + file.getAbsolutePath() + " недоступен для записи.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_update_from_server:
                final String __msgid = msglist.get(mPager.getCurrentItem());
                ArrayList<String> stationsNames = IDECFunctions.getStationsNames();

                final Integer[] chosenStation = {0};

                new AlertDialog.Builder(activity)
                        .setTitle("Выберите станцию")
                        .setSingleChoiceItems(stationsNames.toArray(new CharSequence[stationsNames.size()]), 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                chosenStation[0] = i;
                            }
                        })
                        .setPositiveButton("Начать", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, final int i) {
                                final ProgressDialog progress = ProgressDialog.show(activity, "Загрузка", "Подождите-ка...", true);
                                progress.show();

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Fetcher fetcher = new Fetcher(activity, GlobalTransport.transport);
                                        final boolean result = fetcher.fetch_one_message(__msgid,
                                                Config.values.stations.get(chosenStation[0]));
                                        if (result)
                                            GlobalTransport.transport.setUnread(false, Collections.singletonList(__msgid));

                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                progress.dismiss();
                                                if (!result) {
                                                    new AlertDialog.Builder(activity)
                                                            .setTitle("Ошибка")
                                                            .setMessage("Сообщение не скачалось, посмотрите в системный лог")
                                                            .setPositiveButton(android.R.string.ok, null)
                                                            .show();
                                                } else {
                                                    ((ScreenSlidePagerAdapter) mPager.getAdapter()).mCurrentFragment.initializeMessage(mPager.getContext());
                                                    Toast.makeText(activity, "Сообщение в базе обновлено", Toast.LENGTH_SHORT).show();

                                                    if (listFragment != null) {
                                                        listFragment.mAdapter.messageChanged(__msgid);
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        if (isRealEchoarea)
            EchoReadingPosition.writePositionCache();
        super.onPause();
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public MessageView_full mCurrentFragment;

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            MessageView_full instance = MessageView_full.newInstance(msglist.get(position));
            instance.parentContext = MessageSlideFragment.this;
            return instance;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            mCurrentFragment = ((MessageView_full) object);
            if (starredMenuItem != null)
                setStarredIcon(mCurrentFragment.messageStarred, starredMenuItem);
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public int getCount() {
            return msgCount;
        }
    }
}
