package vit01.idecmobile.Core;

import java.io.Serializable;
import java.util.ArrayList;

public class GlobalConfig implements Serializable {
    public boolean
            defaultEditor = true,
            firstRun = true,
            useProxy = false;

    public int
            oneRequestLimit = 20,
            connectionTimeout = 20;

    public ArrayList<String> offlineEchoareas = new ArrayList<>();
    public ArrayList<Station> stations = new ArrayList<>();

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