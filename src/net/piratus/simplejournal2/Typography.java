package net.piratus.simplejournal2;

import java.util.HashMap;
import java.util.regex.PatternSyntaxException;

/**
 * User: apopovich
 * Date: 20 May 2010
 */
public class Typography {
    public static final HashMap<String, String> REPLACEMENTS;

    static {
        REPLACEMENTS = new HashMap<String, String>();
        REPLACEMENTS.put("(\\s+|^)\"([^\"]+?)\"(\\s+|$|\\.|\\,)", "$1&laquo;$2&raquo;$3");
        REPLACEMENTS.put("\\((tm|тм|TM|ТМ)\\)",	"™");
        REPLACEMENTS.put("\\([cсCС]\\)", "©");
        REPLACEMENTS.put("\\([rRрР]\\)", "®");
        REPLACEMENTS.put("(\\s+|^)-(\\s+)", "$1—$2");
        REPLACEMENTS.put("\\.\\.\\.", "…");
        REPLACEMENTS.put("(\\s+|^)1/2(\\s+|$)", "$1½$2");
        REPLACEMENTS.put("(\\s+|^)1/4(\\s+|$)", "$1¼$2");
        REPLACEMENTS.put("([а-яА-я])\\*([а-яА-я])", "$1’$2")	;
        REPLACEMENTS.put("([^\\!]|^)-{2}([^-]+?)-{2}", "$1<s>$2</s>");

        REPLACEMENTS.put("LJ:([a-zA-Z0-9_]+)", "<lj user=\"$1\"/>");
    }

    public static String parse(String value) {
        String result = value;
        for (String key: REPLACEMENTS.keySet()) {
            try {
                result = result.replaceAll(key, REPLACEMENTS.get(key));
            } catch (PatternSyntaxException e) {
                // This happens sometimes
            }
        }
        return result;
    }
}
