package net.hytaledepot.templates.mod.ui;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class UiModPlugin extends JavaPlugin {
  private enum Lifecycle {
    NEW,
    SETTING_UP,
    RUNNING,
    STOPPING,
    STOPPED,
    FAILED
  }

  private final UiModTemplate service = new UiModTemplate();
  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "hd-ui-mod-heartbeat");
            thread.setDaemon(true);
            return thread;
          });
  private final AtomicLong heartbeatTicks = new AtomicLong();

  private volatile Lifecycle lifecycle = Lifecycle.NEW;
  private volatile ScheduledFuture<?> heartbeatTask;
  private volatile long startedAtEpochMillis;
  private volatile long errorCount;

  public UiModPlugin(JavaPluginInit init) {
    super(init);
  }

  @Override
  protected void setup() {
    lifecycle = Lifecycle.SETTING_UP;

    service.onInitialize(getDataDirectory());

    getCommandRegistry().registerCommand(new UiModOpenCommand());
    getCommandRegistry().registerCommand(new UiModStatusCommand());

    lifecycle = Lifecycle.RUNNING;
  }

  @Override
  protected void start() {
    startedAtEpochMillis = System.currentTimeMillis();

    heartbeatTask =
        scheduler.scheduleAtFixedRate(
            () -> {
              try {
                long tick = heartbeatTicks.incrementAndGet();
                service.onHeartbeat(tick);
                if (tick % 60 == 0) {
                  getLogger().atInfo().log("[UiMod] heartbeat=%d", tick);
                }
              } catch (Exception exception) {
                lifecycle = Lifecycle.FAILED;
                errorCount++;
                getLogger().atInfo().log("[UiMod] heartbeat failed: %s", exception.getMessage());
              }
            },
            1,
            1,
            TimeUnit.SECONDS);

    getTaskRegistry().registerTask(CompletableFuture.completedFuture(null));
  }

  @Override
  protected void shutdown() {
    lifecycle = Lifecycle.STOPPING;
    if (heartbeatTask != null) {
      heartbeatTask.cancel(true);
    }
    scheduler.shutdownNow();
    service.onShutdown();
    lifecycle = Lifecycle.STOPPED;
  }

  private long uptimeSeconds() {
    if (startedAtEpochMillis <= 0L) {
      return 0L;
    }
    return Math.max(0L, (System.currentTimeMillis() - startedAtEpochMillis) / 1000L);
  }

  private final class UiModOpenCommand extends CommandBase {
    private UiModOpenCommand() {
      super("hduimod", "Runs UI mod actions: open, info, heartbeat, toggle, close.");
      setAllowsExtraArguments(true);
      this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void executeSync(CommandContext ctx) {
      if (lifecycle != Lifecycle.RUNNING) {
        ctx.sendMessage(Message.raw("[UiMod] Plugin is not ready yet. lifecycle=" + lifecycle));
        return;
      }

      String action = parseAction(ctx.getInputString(), "open");

      if (("open".equals(action) || "close".equals(action) || "toggle".equals(action)) && !ctx.isPlayer()) {
        ctx.sendMessage(Message.raw("[UiMod] This action requires a player sender."));
        return;
      }

      String result;
      if ("open".equals(action)) {
        result = service.openPage(ctx, heartbeatTicks.get());
      } else {
        String sender = String.valueOf(ctx.sender().getDisplayName());
        result = service.runAction(sender, action, heartbeatTicks.get());
      }
      ctx.sendMessage(Message.raw(result));
    }
  }

  private final class UiModStatusCommand extends CommandBase {
    private UiModStatusCommand() {
      super("hduimodstatus", "Shows diagnostics for UiModPlugin.");
      setAllowsExtraArguments(true);
      this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void executeSync(CommandContext ctx) {
      String sender = String.valueOf(ctx.sender().getDisplayName());
      String line =
          "[UiMod] lifecycle="
              + lifecycle
              + ", uptime="
              + uptimeSeconds()
              + "s"
              + ", heartbeatTicks="
              + heartbeatTicks.get()
              + ", heartbeatActive="
              + (heartbeatTask != null && !heartbeatTask.isCancelled() && !heartbeatTask.isDone())
              + ", openSessions="
              + service.openSessionCount()
              + ", errors="
              + errorCount
              + ", layoutAvailable="
              + service.isLayoutAvailable();
      ctx.sendMessage(Message.raw(line));
      ctx.sendMessage(Message.raw("[UiMod] " + service.diagnostics(sender, heartbeatTicks.get())));
    }
  }

  private static String parseAction(String input, String fallback) {
    String normalized = String.valueOf(input == null ? "" : input).trim();
    if (normalized.isEmpty()) {
      return fallback;
    }

    String[] parts = normalized.split("\\s+");
    String first = parts[0].toLowerCase();
    if (first.startsWith("/")) {
      first = first.substring(1);
    }

    if ((parts.length == 1) && ("hduimod".equals(first) || "uimod".equals(first))) {
      return "open";
    }
    if (parts.length > 1 && first.startsWith("hd")) {
      return parts[1].toLowerCase();
    }
    return first;
  }
}
