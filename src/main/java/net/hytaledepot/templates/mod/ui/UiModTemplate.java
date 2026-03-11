package net.hytaledepot.templates.mod.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class UiModTemplate {
  private static final String LAYOUT_RESOURCE = "Common/UI/Custom/HdUiTemplate.ui";

  private final Map<String, UiModSession> sessions = new ConcurrentHashMap<>();
  private final AtomicLong actionsProcessed = new AtomicLong();
  private final AtomicBoolean demoFlagEnabled = new AtomicBoolean(false);

  private volatile Path dataDirectory;
  private volatile boolean layoutAvailable;
  private volatile long lastHeartbeatTick;

  public void onInitialize(Path dataDirectory) {
    this.dataDirectory = dataDirectory;
    this.layoutAvailable = UiModTemplate.class.getClassLoader().getResource(LAYOUT_RESOURCE) != null;
    this.lastHeartbeatTick = 0L;
  }

  public void onShutdown() {
    sessions.clear();
  }

  public void onHeartbeat(long tick) {
    lastHeartbeatTick = tick;
    if (tick % 120 == 0) {
      sessions.entrySet().removeIf(entry -> !entry.getValue().isOpen());
    }
  }

  public String openPage(CommandContext ctx, long heartbeatTick) {
    if (!ctx.isPlayer()) {
      return "[UiMod] This action requires a player sender.";
    }

    Ref<EntityStore> playerEntityRef = ctx.senderAsPlayerRef();
    if (playerEntityRef == null || !playerEntityRef.isValid()) {
      return "[UiMod] Unable to resolve player reference for this sender.";
    }

    Store<EntityStore> store = playerEntityRef.getStore();
    EntityStore entityStore = store.getExternalData();
    World world = entityStore.getWorld();
    String actor = String.valueOf(ctx.sender().getDisplayName());

    world.execute(
        () -> {
          Player player = store.getComponent(playerEntityRef, Player.getComponentType());
          if (player == null) {
            return;
          }

          PlayerRef playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
          if (playerRef == null) {
            return;
          }

          PageManager pageManager = player.getPageManager();
          if (pageManager == null) {
            return;
          }

          long tick = Math.max(lastHeartbeatTick, heartbeatTick);
          recordAction(actor, "open", tick);
          pageManager.openCustomPage(playerEntityRef, store, new UiModTemplatePage(this, playerRef));
          playerRef.sendMessage(Message.raw("[UiMod] Opened the UI template page."));
        });

    return "[UiMod] Opening the UI template page...";
  }

  public String runAction(String actor, String action, long heartbeatTick) {
    String sender = String.valueOf(actor == null ? "unknown" : actor);
    String normalizedAction = normalizeAction(action);
    UiModSession session = recordAction(sender, normalizedAction, heartbeatTick);

    if ("open".equals(normalizedAction)) {
      return "[UiMod] Open request accepted. sender=" + sender + ", layoutAvailable=" + layoutAvailable;
    }

    if ("close".equals(normalizedAction)) {
      return "[UiMod] Session closed for " + sender;
    }

    if ("toggle".equals(normalizedAction)) {
      boolean enabled = toggleFlag(demoFlagEnabled);
      return "[UiMod] demoFlag=" + enabled + " for " + sender;
    }

    if ("heartbeat".equals(normalizedAction)) {
      return "[UiMod] heartbeatTicks=" + Math.max(lastHeartbeatTick, heartbeatTick) + " for " + sender;
    }

    if ("info".equals(normalizedAction)) {
      return "[UiMod] sessionOpen="
          + session.isOpen()
          + ", lastAction="
          + session.getLastAction()
          + ", openedAtTick="
          + session.getOpenedAtTick();
    }

    return "[UiMod] Unknown action='" + normalizedAction + "' (use: open, info, heartbeat, toggle, close).";
  }

  UiModSession recordAction(String actor, String action, long heartbeatTick) {
    actionsProcessed.incrementAndGet();
    UiModSession session = sessions.computeIfAbsent(String.valueOf(actor), key -> new UiModSession(key, heartbeatTick));
    session.mark(action, heartbeatTick);
    return session;
  }

  public String diagnostics(String sender, long heartbeatTicks) {
    String actor = String.valueOf(sender == null ? "unknown" : sender);
    UiModSession session = sessions.get(actor);
    String directory = dataDirectory == null ? "unset" : dataDirectory.toString();
    if (session == null) {
      return "session=none, openSessions=" + openSessionCount() + ", actionsProcessed=" + actionsProcessed.get() + ", dataDirectory=" + directory;
    }

    return "sessionOpen="
        + session.isOpen()
        + ", lastUiAction="
        + session.getLastAction()
        + ", openedAtTick="
        + session.getOpenedAtTick()
        + ", openSessions="
        + openSessionCount()
        + ", actionsProcessed="
        + actionsProcessed.get()
        + ", heartbeatTicks="
        + Math.max(lastHeartbeatTick, heartbeatTicks)
        + ", dataDirectory="
        + directory;
  }

  public int openSessionCount() {
    int count = 0;
    for (UiModSession session : sessions.values()) {
      if (session.isOpen()) {
        count++;
      }
    }
    return count;
  }

  public boolean isLayoutAvailable() {
    return layoutAvailable;
  }

  public long getLastHeartbeatTick() {
    return lastHeartbeatTick;
  }

  public boolean isDemoFlagEnabled() {
    return demoFlagEnabled.get();
  }

  private static boolean toggleFlag(AtomicBoolean flag) {
    while (true) {
      boolean current = flag.get();
      boolean next = !current;
      if (flag.compareAndSet(current, next)) {
        return next;
      }
    }
  }

  private static String normalizeAction(String action) {
    String normalized = String.valueOf(action == null ? "" : action).trim().toLowerCase();
    return normalized.isEmpty() ? "open" : normalized;
  }
}
