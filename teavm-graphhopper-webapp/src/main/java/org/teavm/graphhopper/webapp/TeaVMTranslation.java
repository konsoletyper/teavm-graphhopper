package org.teavm.graphhopper.webapp;

import java.util.Locale;
import java.util.Map;
import com.graphhopper.util.Translation;

/**
 *
 * @author Alexey Andreev
 */
public class TeaVMTranslation implements Translation {
    @Override
    public String tr(String key, Object... params) {
        return key;
    }

    @Override
    public Map<String, String> asMap() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public String getLanguage() {
        return "en_US";
    }
}
