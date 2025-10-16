package com.splitmanager.models;

import lombok.Getter;
import lombok.Setter;

/**
 * Simple immutable container describing a suggested transfer between two players.
 */
@Getter
@Setter
public final class Transfer
{
	/**
	 * Player paying or sending value.
	 */
	final String from;
	/**
	 * Player receiving value.
	 */
	final String to;
	/**
	 * Amount in coins (or the configured split unit).
	 */
	final long amount;

	/**
	 * Create a new suggested transfer record.
	 *
	 * @param from   player paying/sending
	 * @param to     player receiving
	 * @param amount amount in coins
	 */
	public Transfer(String from, String to, long amount)
	{
		this.from = from;
		this.to = to;
		this.amount = amount;
	}
}
