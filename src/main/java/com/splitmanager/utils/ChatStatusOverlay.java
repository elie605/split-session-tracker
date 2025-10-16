package com.splitmanager.utils;

import com.splitmanager.PluginConfig;
import java.awt.Color;
import java.awt.Dimension;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class ChatStatusOverlay extends OverlayPanel
{
	private final PluginConfig config;
	private boolean visible = false;
	private boolean chatchanOn = false;
	private boolean clanOn = false;
	private boolean guestOn = false;
	private boolean countedOn = false;

	public ChatStatusOverlay(PluginConfig config)
	{
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	public void setVisible(boolean v)
	{
		this.visible = v;
	}

	/**
	 * fc/clan/guest raw states + combined "counted" state (based on config toggles)
	 */
	public void setStatuses(boolean chatchan, boolean clan, boolean guest, boolean counted)
	{
		this.chatchanOn = chatchan;
		this.clanOn = clan;
		this.guestOn = guest;
		this.countedOn = counted;
	}

	@Override
	public Dimension render(java.awt.Graphics2D g)
	{
		if (!visible)
		{
			return null;
		}
		if (chatchanOn == true)
		{
			return null;
		}
		if (chatchanOn == false)
		{
			panelComponent.getChildren().clear();
			panelComponent.setPreferredSize(new Dimension(230, 0));

			final String title = "WARNING! NOT IN FC!";
			final Color titleColor = new Color(255, 80, 80);

			panelComponent.getChildren().add(TitleComponent.builder()
				.text(title)
				.color(titleColor)
				.build());

			addStatusLine("Chat Channel", chatchanOn);

			return super.render(g);
		}
		return null;
	}

	public void addStatusLine(String label, boolean on)
	{
		final String statusText = on ? "ON" : "OFF";
		final Color statusCol = on ? new Color(120, 255, 120) : new Color(255, 120, 120);

		panelComponent.getChildren().add(LineComponent.builder()
			.left(label)
			.right(statusText)
			.leftColor(Color.WHITE)
			.rightColor(statusCol)
			.build());
	}


}