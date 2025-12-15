package com.splitmanager.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.swing.JFormattedTextField;

public class Formats
{
	private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
		.withZone(ZoneId.systemDefault());
	private static final DecimalFormat DF = new DecimalFormat("#,##0");
	private static final DecimalFormat DF_3DP = new DecimalFormat("#,##0.###");

	public static DateTimeFormatter getDateTime()
	{
		return TS;
	}

	public static DecimalFormat getDecimalFormat()
	{
		return DF;
	}

	public static final class OsrsAmountFormatter extends JFormattedTextField.AbstractFormatter
	{
		private static final Pattern P =
			Pattern.compile("(?i)^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([kmb]| coins)?\\s*$");

		private static BigDecimal getBigDecimal(BigDecimal number, char suffix) throws ParseException
		{
			if (number.signum() < 0)
			{
				throw new ParseException("Negative not allowed", 0);
			}

			long multiplierK;
			switch (suffix)
			{
				case 'k':
					multiplierK = 1_000L;
					break;
				case 'm':
					multiplierK = 1_000_000L;
					break;
				case 'b':
					multiplierK = 1_000_000_000L;
					break;
				default:
					multiplierK = 1L;
					break;
			}

			return number.multiply(BigDecimal.valueOf(multiplierK));
		}

		/**
		 * Convert a K-based long amount to a human string with a target suffix.
		 * Examples:
		 * amountK=1500, 'm' -> "1.5M"
		 * amountK=250,  'k' -> "250K"
		 * amountK=3,    'b' -> "0.003B"
		 *
		 * @param amountK value expressed in K-units (thousands)
		 * @param suffix  desired suffix: 'k', 'm', or 'b' (case-insensitive)
		 * @return formatted string with suffix
		 */
		public static String toSuffixString(long amountK, char suffix)
		{
			DecimalFormat localDF = DF;
			String s = String.valueOf(Character.toLowerCase(suffix));
			long divK; // how many K per target unit
			switch (s)
			{
				case "k":
					divK = 1_000L;
					break;              // 1 K per K
				case "m":
					divK = 1_000_000L;
					break;          // 1,000 K per M
				case "b":
					divK = 1_000_000_000L;
					localDF = DF_3DP;
					break;      // 1,000,000 K per B
				default:
					divK = 1L;
					s = "gp";
					break;
			}

			// Use BigDecimal to avoid precision issues for large numbers
			BigDecimal val = BigDecimal.valueOf(amountK)
				.divide(BigDecimal.valueOf(divK));

			// Format with up to 3 decimals, trimming trailing zeros
			String num = localDF.format(val);

			// Uppercase the suffix in output
			return num + s.toUpperCase();
		}

		/**
		 * Overload that accepts a String suffix ("k", "m", "b").
		 */
		public static String toSuffixString(long amountK, String suffix)
		{
			if (suffix == null || suffix.isEmpty())
			{
				return toSuffixString(amountK, 'k');
			}
			return toSuffixString(amountK, suffix.charAt(0));
		}

		@Override
		public Object stringToValue(@Nonnull String text) throws ParseException
		{
			String s = text.replace(",", "").trim(); // ignore commas
			if (s.isEmpty())
			{
				return null;
			}

			java.util.regex.Matcher m = P.matcher(s);
			if (!m.matches())
			{
				throw new ParseException("Invalid amount", 0);
			}

			BigDecimal number = new BigDecimal(m.group(1));
			char suffix = (m.group(2) == null) ? ' ' : Character.toLowerCase(m.group(2).charAt(0));

			// Convert to raw coins
			BigDecimal coinsValue = getBigDecimal(number, suffix);
			// Return the exact long value (no normalization to K units)
			return coinsValue.setScale(0, RoundingMode.FLOOR).longValueExact();
		}

		@Override
		public String valueToString(Object value)
		{
			if (value == null)
			{
				return "";
			}
			Long k = ((Number) value).longValue();
			return k + "K";
		}

	}

}

