package vit01.idecmobile.Core;

import java.io.Serializable;
import java.util.ArrayList;

public class GlobalConfig implements Serializable {
    static final long serialVersionUID = 1L;
    public boolean
            defaultEditor = true,
            firstRun = true,
            useProxy = false,
            useTor = false,
            oldQuote = false, // упрощённое (старое) цитирование
            notificationsEnabled = false,
            notificationsVibrate = true,
            swipeToFetch = true;
    public int
            oneRequestLimit = 20,
            connectionTimeout = 20,
            carbon_limit = 50, // максимальное количество сообщений в карбонке
            notifyFireDuration = 15, // интервал проверки для уведомлений
            proxyType = 1; // 0 - Socks, 1 - HTTP
    public ArrayList<String> offlineEchoareas = new ArrayList<>();
    public ArrayList<Station> stations = new ArrayList<>();

    // Сообщения какого пользователя слать в карбонку
    public String carbon_to = "All", // разделять двоеточием
            proxyAddress = "127.0.0.1:9050", // аутентификация для http-прокси поддерживается
            applicationTheme = "default"; // тема оформления

    GlobalConfig() {
        offlineEchoareas.add("lenta.rss");
        offlineEchoareas.add("edgar.allan.poe");

        stations.add(new Station());
        stations.add(new Station());

        Station secondStation = stations.get(1);
        secondStation.nodename = "tavern";
        secondStation.echoareas.add(0, "spline.local.14");
        swipeToFetch = true;
    }
}