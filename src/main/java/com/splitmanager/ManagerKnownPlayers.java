package com.splitmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.splitmanager.utils.InstantTypeAdapter;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;

/**
 * This class provides functionality to manage relationships between "main" players
 * and their alternate (alt) players within a gaming or user system. It interacts
 * with backend session management to perform operations like linking, unlinking,
 * and retrieving alt players while also updating the user interface to reflect
 * these changes.
 */
@Singleton
public class ManagerKnownPlayers
{
	private final Gson gson;
	private final PluginConfig config;
	@Getter
	private Set<String> knownPlayers = new LinkedHashSet<>();
	@Getter
	private Map<String, String> altMainMapping = new LinkedHashMap<>();

	@Inject
	public ManagerKnownPlayers(PluginConfig config)
	{
		this.config = config;
		this.gson = new GsonBuilder()
			.registerTypeAdapter(Instant.class, new InstantTypeAdapter())
			.create();
	}

	public void loadFromConfig()
	{
		knownPlayers.clear();
		String csv = config.knownPlayersCsv();
		if (csv != null && !csv.isEmpty())
		{
			for (String p : csv.split(","))
			{
				String t = p.trim();
				if (!t.isEmpty())
				{
					knownPlayers.add(t);
				}
			}
		}

		altMainMapping.clear();
		String altsJson = config.altsJson();
		if (altsJson != null && !altsJson.isEmpty())
		{
			try
			{
				Type mapType = new TypeToken<Map<String, String>>()
				{
				}.getType();
				Map<String, String> m = gson.fromJson(altsJson, mapType);
				if (m != null)
				{
					altMainMapping.putAll(m);
				}
			}
			catch (Exception ignored)
			{
			}
		}
	}

	public void saveToConfig()
	{
		config.knownPlayersCsv(String.join(",", knownPlayers));
		// save alt mapping
		try
		{
			config.altsJson(gson.toJson(altMainMapping));
		}
		catch (Exception e)
		{
			// ignore
		}
	}

	/**
	 * Parse the alt name from a selected entry.
	 */
	public String parseAltFromEntry(String selectedEntry)
	{
		if (selectedEntry.contains(" is an alt of "))
		{
			String[] parts = selectedEntry.split(" is an alt of ", 2);
			if (parts.length == 2)
			{
				return parts[0].trim();
			}
		}
		return selectedEntry.trim();
	}

	/**
	 * @return unmodifiable set of known players that are mains (exclude names mapped as alts).
	 */
	public Set<String> getKnownMains()
	{
		LinkedHashSet<String> mains = knownPlayers.stream()
			.filter(p -> !isAlt(p))
			.collect(Collectors.toCollection(LinkedHashSet::new));
		return Collections.unmodifiableSet(mains);
	}

	/**
	 * Validate whether an alt may be linked to a main.
	 * Rules:
	 * - alt and main must be non-empty and not equal (case-insensitive)
	 * - the chosen main cannot itself be an alt
	 * - the alt cannot already point to a different main
	 * - the alt cannot be someone else's main (prevents cycles)
	 *
	 * @return true if the link is allowed
	 */
	public boolean canLinkAltToMain(String alt, String main)
	{
		if (alt == null || main == null)
		{
			return false;
		}
		String a = alt.trim();
		String m = main.trim();
		if (a.isEmpty() || m.isEmpty())
		{
			return false;
		}
		if (a.equalsIgnoreCase(m))
		{
			return false; // cannot link self
		}
		// main cannot be an alt
		if (altMainMapping.containsKey(m))
		{
			return false;
		}
		// alt cannot already have a different main
		if (altMainMapping.containsKey(a) && !altMainMapping.get(a).equalsIgnoreCase(m))
		{
			return false;
		}
		// alt cannot be a main of others (prevents main being alt through this change)
		for (Map.Entry<String, String> e : altMainMapping.entrySet())
		{
			if (e.getValue() != null && e.getValue().equalsIgnoreCase(a))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Attempt to persist an alt->main link, enforcing the validation from canLinkAltToMain().
	 * Adds both names to the known list on success.
	 *
	 * @return true if the mapping either already existed (same) or was created
	 */
	public boolean trySetAltMain(String alt, String main)
	{
		if (!canLinkAltToMain(alt, main))
		{
			return false;
		}
		String a = alt.trim();
		String m = main.trim();
		// no-op if already linked
		if (altMainMapping.containsKey(a) && altMainMapping.get(a).equalsIgnoreCase(m))
		{
			return true;
		}
		altMainMapping.put(a, m);
		knownPlayers.add(a);
		knownPlayers.add(m);
		saveToConfig();
		return true;
	}

	/**
	 * Unlink the given alt from its main.
	 *
	 * @param alt the alt name to unlink
	 * @return true if an existing mapping was removed
	 */
	public boolean unlinkAlt(String alt)
	{
		if (alt == null || alt.trim().isEmpty())
		{
			return false;
		}
		String a = alt.trim();
		if (!altMainMapping.containsKey(a))
		{
			return false;
		}
		altMainMapping.remove(a);
		saveToConfig();
		return true;
	}

	/**
	 * Resolve a display name to its main account by following the alt->main mapping.
	 * If the name is not an alt, returns the original trimmed name. Protects against cycles.
	 *
	 * @param name main or alt name
	 * @return resolved main name (or the input trimmed if not an alt)
	 */
	public String getMainName(String name)
	{
		if (name == null)
		{
			return null;
		}
		String n = name.trim();
		String visited = null;
		// resolve chain up to a few steps to avoid cycles
		for (int i = 0; i < 5; i++)
		{
			String m = altMainMapping.get(n);
			if (m == null || m.equalsIgnoreCase(n))
			{
				return n;
			}
			if (visited != null && visited.equalsIgnoreCase(m))
			{
				break;
			}
			visited = n;
			n = m;
		}
		return n;
	}

	/**
	 * @param name player name
	 * @return true if the given name is present as a key in the alt->main mapping
	 */
	public boolean isAlt(String name)
	{
		if (name == null)
		{
			return false;
		}
		return altMainMapping.containsKey(name.trim());
	}

	/**
	 * List all alts currently linked to the given main.
	 *
	 * @param main main account name
	 * @return sorted list of alt names linked to this main (case-insensitive compare)
	 */
	public List<String> getAltsOf(String main)
	{
		if (main == null || main.isBlank())
		{
			return List.of();
		}
		String m = main.trim();
		List<String> out = new ArrayList<>();
		for (Map.Entry<String, String> e : altMainMapping.entrySet())
		{
			if (e.getValue() != null && e.getValue().equalsIgnoreCase(m))
			{
				out.add(e.getKey());
			}
		}
		out.sort(String::compareToIgnoreCase);
		return out;
	}


	/**
	 * Add a player name to the known-players list.
	 *
	 * @param name display name
	 * @return true if added
	 */
	public boolean addKnownPlayer(String name)
	{
		boolean added = knownPlayers.add(name.trim());
		if (added)
		{
			saveToConfig();
		}
		return added;
	}

	public boolean removeKnownPlayer(String name)
	{
		String n = name == null ? null : name.trim();
		if (n == null || n.isEmpty())
		{
			return false;
		}
		boolean rem = knownPlayers.remove(n);

		altMainMapping.remove(n);
		altMainMapping.entrySet().removeIf(e -> e.getValue().equalsIgnoreCase(n));

		saveToConfig();
		return rem;
	}

	public void init()
	{
		loadFromConfig();
	}
}