package vit01.idecmobile.Core;

public class DraftMessage {
    public String echo, to, subj, repto, msg;

    public DraftMessage() {
        echo = "no.echo";
        to = "All";
        subj = "...";
        repto = null;
        msg = "";
    }

    public DraftMessage(String raw) {
        this();
        String[] pieces = raw.split("\n");

        if (pieces.length >= 5) {
            echo = pieces[0];
            to = pieces[1];
            subj = pieces[2];
            int start;

            if (pieces[4].startsWith("@repto:")) {
                repto = pieces[4].substring(7);
                start = 5;
            } else {
                start = 6;
            }

            for (int i = start; i < pieces.length - 1; i++) {
                msg += pieces[i] + "\n";
            }
            msg += pieces[pieces.length - 1];
        }
    }

    public String raw() {
        String result = echo + "\n" + to + "\n" + subj + "\n\n";
        if (repto != null) {
            result += "@repto:" + repto + "\n";
        }
        result += msg;

        return result;
    }
}