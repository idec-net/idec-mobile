package vit01.idecmobile.Core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

public class Station implements Serializable {
    static final long serialVersionUID = 100L;

    public String
            nodename = "New Station",
            address = "https://ii-net.tk/ii/ii-point.php?q=/",
            authstr = "";
    public ArrayList<String> echoareas = new ArrayList<>();
    public boolean
            fetch_enabled = true,
            xc_enable = true,
            advanced_ue = true,
            pervasive_ue = false;
    public int
            ue_limit = 5,
            cut_remote_index = 0;

    public Station() {
        String[] default_echoareas = new String[]{"pipe.2032", "ii.14", "mlp.15", "ii.test.14", "piratemedia.rss.15",
                "habra.16", "test.15", "develop.16", "linux.14"};

        Collections.addAll(echoareas, default_echoareas);
    }
}