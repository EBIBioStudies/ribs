package uk.ac.ebi.biostudies.api.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class StudyUtils {

    private static final Logger LOGGER = LogManager.getLogger(StudyUtils.class.getName());
    private static ScriptEngine jsEngine = new ScriptEngineManager().getEngineByName("nashorn");

    public static String escape(String s) {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '[' || c == ']' || c == '"' || c == '{' || c == '}' || c == '~' || c == '?' || c == '|' || c == '&' || c == '/') {
                sb.append('\\');
            }

            sb.append(c);
        }

        return sb.toString();
    }

    public static String decodeForFireBug(String inputStr) {
        try {
            return jsEngine.eval(String.format("unescape(encodeURIComponent('%s'))", inputStr)).toString().replace("#", "%23").replace("+", "%2B").replace("=", "%3D").replace("@", "%40").replace("$", "%24").toString();
        } catch (ScriptException e) {
            LOGGER.error(inputStr + " problem in unescapeing for Fire encoding bug", e);
        }
        return inputStr;
    }
}