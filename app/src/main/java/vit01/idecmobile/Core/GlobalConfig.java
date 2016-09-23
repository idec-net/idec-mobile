package vit01.idecmobile.Core;

import java.io.Serializable;
import java.util.ArrayList;

public class GlobalConfig implements Serializable {
    static final long serialVersionUID = 1L;
    public boolean
            defaultEditor = true,
            firstRun = true,
            useProxy = false,
            oldQuote = false; // упрощённое (старое) цитирование
    public int
            oneRequestLimit = 20,
            connectionTimeout = 20,
            carbon_limit = 50; // максимальное количество сообщений в карбонке
    public ArrayList<String> offlineEchoareas = new ArrayList<>();
    public ArrayList<Station> stations = new ArrayList<>();

    // Сообщения какого пользователя слать в карбонку
    public String carbon_to = "All"; // разделять двоеточием

    GlobalConfig() {
        offlineEchoareas.add("lenta.rss");
        offlineEchoareas.add("edgar.allan.poe");

        stations.add(new Station());
        stations.add(new Station());

        Station secondStation = stations.get(1);
        secondStation.nodename = "tavern";
        secondStation.echoareas.add(0, "spline.local.14");
    }
}