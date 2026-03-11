package net.hytaledepot.templates.mod.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class UiModTemplatePage extends InteractiveCustomUIPage<UiModTemplatePage.EventDataPayload> {
  private static final String PAGE_LAYOUT = "HdUiTemplate.ui";

  private final UiModTemplate uiService;
  private final PlayerRef playerRef;

  public UiModTemplatePage(UiModTemplate uiService, PlayerRef playerRef) {
    super(playerRef, CustomPageLifetime.CanDismiss, EventDataPayload.CODEC);
    this.uiService = uiService;
    this.playerRef = playerRef;
  }

  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events, Store<EntityStore> store) {
    commands.append(PAGE_LAYOUT);
    commands.set("#StatusLine.Text", buildStatusLine());
    commands.set("#HintLine.Text", "Use the buttons to test state, heartbeat, and session updates.");

    events.addEventBinding(CustomUIEventBindingType.Activating, "#InfoBtn", EventData.of("Action", "info"));
    events.addEventBinding(CustomUIEventBindingType.Activating, "#HeartbeatBtn", EventData.of("Action", "heartbeat"));
    events.addEventBinding(CustomUIEventBindingType.Activating, "#ToggleBtn", EventData.of("Action", "toggle"));
    events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseBtn", EventData.of("Action", "close"));
  }

  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, EventDataPayload data) {
    if (data == null || data.action == null) {
      return;
    }

    String actor = String.valueOf(playerRef.getUsername());
    String action = data.action.trim().toLowerCase();
    long heartbeatTick = uiService.getLastHeartbeatTick();

    if ("close".equals(action)) {
      uiService.recordAction(actor, "close", heartbeatTick);
      close();
      return;
    }

    String response = uiService.runAction(actor, action, heartbeatTick);
    playerRef.sendMessage(Message.raw(response));
    rebuild();
  }

  private String buildStatusLine() {
    return "demoFlag="
        + uiService.isDemoFlagEnabled()
        + " | heartbeat="
        + uiService.getLastHeartbeatTick()
        + " | openSessions="
        + uiService.openSessionCount();
  }

  public static final class EventDataPayload {
    public static final BuilderCodec<EventDataPayload> CODEC =
        BuilderCodec.builder(EventDataPayload.class, EventDataPayload::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (data, value) -> data.action = value, data -> data.action)
            .add()
            .build();

    public String action;
  }
}
