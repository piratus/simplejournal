package net.piratus.simplejournal.livejournal;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * User: piratus
 * Date: Jun 20, 2010
 */
public class LJUtilTest {

    @Test
    public void testGetDateFromLJString() throws Exception {
        final String dateString = "2002-07-14 11:17:00";

        final HashMap<String, Object> result = LJUtil.getDateFromLJString(dateString);
        assertNotNull(result);
        assertEquals("2002", result.get(LJUtil.YEAR));
        assertEquals("07", result.get(LJUtil.MONTH));
        assertEquals("14", result.get(LJUtil.DAY));
        assertEquals("11", result.get(LJUtil.HOUR));
        assertEquals("17", result.get(LJUtil.MIN));
    }
}
