package com.example.pksession;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class Formats {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final DecimalFormat DF = new DecimalFormat("#,##0");

    public static DateTimeFormatter getDateTime(){
        return TS;
    }
    public static DecimalFormat getDecimalFormat(){
        return DF;
    }

    public static final class OsrsAmountFormatter extends JFormattedTextField.AbstractFormatter {
        private static final Pattern P =
                Pattern.compile("(?i)^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([kmb])?\\s*$");

        @Override
        public Object stringToValue(String text) throws ParseException {
            if (text == null) return null;
            String s = text.replace(",", "").trim(); // ignore commas
            if (s.isEmpty()) return null;

            java.util.regex.Matcher m = P.matcher(s);
            if (!m.matches()) throw new ParseException("Invalid amount", 0);

            BigDecimal number = new BigDecimal(m.group(1));
            char suffix = (m.group(2) == null) ? 'k' : Character.toLowerCase(m.group(2).charAt(0));

            BigDecimal kVal = getBigDecimal(number, suffix);
            // normalize to whole K (floor)
            return kVal.setScale(0, RoundingMode.FLOOR).longValueExact();
        }

        private static BigDecimal getBigDecimal(BigDecimal number, char suffix) throws ParseException {
            if (number.signum() < 0) throw new ParseException("Negative not allowed", 0);

            long multiplierK;
            switch (suffix) {
                case 'k':
                    multiplierK = 1L;            // thousands
                    break;
                case 'm':
                    multiplierK = 1_000L;        // millions -> K
                    break;
                case 'b':
                    multiplierK = 1_000_000L;    // billions -> K
                    break;
                default:
                    multiplierK = 1L;
            }

            return number.multiply(BigDecimal.valueOf(multiplierK));
        }

        @Override
        public String valueToString(Object value){
            if (value == null) return "";
            Long k = ((Number) value).longValue();
            return k + "K";
        }
    }

}

