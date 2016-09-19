package vit01.idecmobile.Core;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Config {
    public static GlobalConfig values;
    public static String filename = "config.obj";

    public static void loadConfig(Context context) {
        try {
            FileInputStream is = context.openFileInput(filename);
            ObjectInputStream ois = new ObjectInputStream(is);
            values = (GlobalConfig) ois.readObject();
            ois.close();
            is.close();
            configUpdate(context);
        } catch (Exception e) {
            SimpleFunctions.debug("Конфиг не найден/ошибка, создаём по умолчанию");
            e.printStackTrace();

            values = new GlobalConfig();
        }
    }

    public static void writeConfig(Context context) {
        try {
            FileOutputStream os = context.openFileOutput(filename, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(values);
            oos.close();
            os.close();
        } catch (Exception e) {
            SimpleFunctions.debug("Ошибка записи конфига");
            e.printStackTrace();
        }
    }

    public static void configUpdate(Context context) {
        if (Config.values.carbon_to == null) {
            Config.values.carbon_to = "All";
        }

        if (Config.values.carbon_limit <= 0) {
            Config.values.carbon_limit = 50;
        }

        boolean needForWrite = false;

        for (Station station : Config.values.stations) {
            if (station.outbox_storage_id == null) {
                station.outbox_storage_id = SimpleFunctions.getRandomUUID();
                needForWrite = true;
            }
        }

        if (needForWrite) writeConfig(context);
    }
}
