package utils;

/*
 * net/balusc/util/DateUtil.java
 *
 * Copyright (C) 2007 BalusC
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Useful Date utilities.
 *
 * @author BalusC
 * @see CalendarUtil
 * @link http://balusc.blogspot.com/2007/09/dateutil.html
 */
public final class DateUtil {

    // Init
    // ---------------------------------------------------------------------------------------
    private static final HashMap<String, String> DATE_FORMAT_REGEXPS = new HashMap<String, String>() {
        private static final long serialVersionUID = 8244050687250330027L;

        {
            put("^\\d{8}$", "yyyyMMdd");
            put("^\\d{1,2}-\\d{1,2}-\\d{4}$", "MM-dd-yyyy");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd");
            put("^\\d{4}-\\d{1,2}$", "yyyy-MM");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
            put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$", "dd MMM yyyy");
            put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
            put("^\\d{12}$", "yyyyMMddHHmm");
            put("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm");
            put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$",
                    "dd-MM-yyyy HH:mm");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$",
                    "MM/dd/yyyy HH:mm");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$",
                    "yyyy-MM-dd HH:mm");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$",
                    "yyyy/MM/dd HH:mm");
            put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$",
                    "dd MMM yyyy HH:mm");
            put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$",
                    "dd MMMM yyyy HH:mm");
            put("^\\d{14}$", "yyyyMMddHHmmss");
            put("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss");
            put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
                    "dd-MM-yyyy HH:mm:ss");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}.\\d{3}$",
                    "yyyy-MM-dd HH:mm:ss.S");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$",
                    "yyyy-MM-dd HH:mm:ss");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
                    "MM/dd/yyyy HH:mm:ss");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$",
                    "yyyy/MM/dd HH:mm:ss");
            put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
                    "dd MMM yyyy HH:mm:ss");
            put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
                    "dd MMMM yyyy HH:mm:ss");
            put("^\\d{4}-W\\d{1,2}-\\d{1,2}$", "YYYY-'W'ww-u");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}$",
                    "yyyy-MM-dd'T'HH:mm:ss");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}.\\d{3}$",
                    "yyyy-MM-dd'T'HH:mm:ss.S");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}.\\d{3}[+ -]\\d{2}:\\d{2}$",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}.\\d{3}[+ -]\\d{4}$",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            put("^\\d{12}[+ -]\\d{4}$", "yyMMddHHmmssZ");
            put("^[A-Z a-z]{3},\\s\\d{1,2}\\s[A-Z a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}\\s[+ -]\\d{4}$",
                    "EEE, d MMM yyyy HH:mm:ss Z");
            put("^\\d{5}.[A-Z a-z]{4,9}.\\d{1,2}\\s[A-Z]{2}\\s\\d{1,2}:\\d{1,2}\\s[A-Z]{2}$",
                    "yyyyy.MMMMM.dd GGG hh:mm aaa");
            put("^\\d{1,2}:\\d{1,2}\\s[A-Z]{2},\\s[A-Z]{3}$", "K:mm a, z");
            put("^\\d{1,2}\\so'clock\\s[A-Z]{2},\\s[A-Z a-z]{1,}$",
                    "hh 'o''clock' a, zzzz");
            put("^\\d{1,2}:\\d{1,2}\\s[A-Z]{2}$", "h:mm a");
            put("^[A-Z a-z]{3}\\s[A-Z a-z]{3}\\s\\d{1,2}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}\\s[+ -]\\d{4}\\s\\d{4}$",
                    "EEE MMM d HH:mm:ss Z yyyy");
        }
    };

    /**
     * Determine SimpleDateFormat pattern matching with the given date string.
     * Returns null if format is unknown. You can simply extend DateUtil with
     * more formats if needed.
     *
     * @param dateString
     *            The date string to determine the SimpleDateFormat pattern for.
     * @return The matching SimpleDateFormat pattern, or null if format is
     *         unknown.
     * @see SimpleDateFormat
     */
    public static String determineDateFormat(String dateString) {
        for (String regexp : DATE_FORMAT_REGEXPS.keySet())
            if (dateString.matches(regexp))
                return DATE_FORMAT_REGEXPS.get(regexp);
        return null; // Unknown format.
    }

    // Converters
    // ---------------------------------------------------------------------------------

    /**
     * Checks whether the actual date of the given date string is valid. This
     * makes use of the {@link DateUtil#determineDateFormat(String)} to
     * determine the SimpleDateFormat pattern to be used for parsing.
     *
     * @param dateString
     *            The date string.
     * @return True if the actual date of the given date string is valid.
     */
    public static boolean isValidDate(String dateString) {
        try {
            parse(dateString);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Checks whether the actual date of the given date string is valid based on
     * the given date format pattern.
     *
     * @param dateString
     *            The date string.
     * @param dateFormat
     *            The date format pattern which should respect the
     *            SimpleDateFormat rules.
     * @return True if the actual date of the given date string is valid based
     *         on the given date format pattern.
     * @see SimpleDateFormat
     */
    public static boolean isValidDate(String dateString, String dateFormat) {
        try {
            parse(dateString, dateFormat);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    // Validators
    // ---------------------------------------------------------------------------------

    /**
     * Parse the given date string to date object and return a date instance
     * based on the given date string. This makes use of the
     * {@link DateUtil#determineDateFormat(String)} to determine the
     * SimpleDateFormat pattern to be used for parsing.
     *
     * @param dateString
     *            The date string to be parsed to date object.
     * @return The parsed date object.
     * @throws ParseException
     *             If the date format pattern of the given date string is
     *             unknown, or if the given date string or its actual date is
     *             invalid based on the date format pattern.
     */
    public static Date parse(String dateString) throws ParseException {
        try {
            DateFormat df = DateFormat.getDateInstance();
            return df.parse(dateString);
        } catch (ParseException e) {
            try {
                DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT,
                        new Locale("pt", "BR"));
                return df.parse(dateString);
            } catch (ParseException e1) {
                // TODO: ' - Incluir mensagem de log com nível de INFO para
                // registrar que o formato
                // de datas informado não tem cobertura pelo harpia, sendo
                // necessário incluir um novo
                // registro no hashmap DATE_FORMAT_REGEXPS com o regex para esse
                // novo formato.
                String dateFormat = determineDateFormat(dateString);

                if (dateFormat == null)
                    throw new ParseException(
                            "Unknown date format for " + dateString + ".", 0);
                return parse(dateString, dateFormat);
            }
        }
    }

    /**
     * Validate the actual date of the given date string based on the given date
     * format pattern and return a date instance based on the given date string.
     *
     * @param dateString
     *            The date string.
     * @param dateFormat
     *            The date format pattern which should respect the
     *            SimpleDateFormat rules.
     * @return The parsed date object.
     * @throws ParseException
     *             If the given date string or its actual date is invalid based
     *             on the given date format pattern.
     * @see SimpleDateFormat
     */
    public static Date parse(String dateString, String dateFormat)
            throws ParseException {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat,
                    Locale.ENGLISH);
            simpleDateFormat.setLenient(false); // Don't automatically convert
                                                // invalid date.
            return simpleDateFormat.parse(dateString);
        } catch (ParseException e) {
            try {
                DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT,
                        new Locale("pt", "BR"));
                return df.parse(dateString);
            } catch (NullPointerException | IllegalArgumentException
                    | ParseException e1) {
                throw new ParseException("Unknown date format (" + dateFormat
                        + ")  for " + dateString + ".", 0);
            }
        }
    }

    // Checkers
    // -----------------------------------------------------------------------------------

    private DateUtil() {
        // Utility class, hide the constructor.
    }
}
