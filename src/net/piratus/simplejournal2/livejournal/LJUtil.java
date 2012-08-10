package net.piratus.simplejournal2.livejournal;

import java.util.Calendar;
import java.util.HashMap;

/**
 * User: piratus
 * Date: Jun 20, 2010
 */
public class LJUtil {
    public static final String YEAR = "year";
    public static final String MONTH = "mon";
    public static final String DAY = "day";
    public static final String HOUR = "hour";
    public static final String MIN = "min";

    public static HashMap<String, Object> getDate(Calendar calendar) {
        final HashMap<String, Object> date = new HashMap<String, Object>();
        date.put(YEAR, calendar.get(Calendar.YEAR));
        date.put(MONTH, calendar.get(Calendar.MONTH) + 1);
        date.put(DAY, calendar.get(Calendar.DAY_OF_MONTH));
        date.put(HOUR, calendar.get(Calendar.HOUR_OF_DAY));
        date.put(MIN, calendar.get(Calendar.MINUTE));

        return date;
    }

    public static HashMap<String, Object> getDateFromLJString(String dateString) {
        final HashMap<String, Object> result = new HashMap<String, Object>();

        if (dateString == null || dateString.length() == 0) {
            return result;
        }

        final String[] dateTime = dateString.split(" ");
        final String[] dateItems = dateTime[0].split("-");
        result.put(YEAR, dateItems[0]);
        result.put(MONTH, dateItems[1]);
        result.put(DAY, dateItems[2]);

        final String[] timeItems = dateTime[1].split(":");
        result.put(HOUR, timeItems[0]);
        result.put(MIN, timeItems[1]);

        return result;
    }
}
