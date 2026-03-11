package net.hytaledepot.templates.mod.ui;

public final class UiModSession {
  private final String actor;
  private long openedAtTick;
  private String lastAction;
  private boolean open;

  public UiModSession(String actor, long openedAtTick) {
    this.actor = String.valueOf(actor);
    this.openedAtTick = openedAtTick;
    this.lastAction = "open";
    this.open = true;
  }

  public String getActor() {
    return actor;
  }

  public long getOpenedAtTick() {
    return openedAtTick;
  }

  public String getLastAction() {
    return lastAction;
  }

  public boolean isOpen() {
    return open;
  }

  public void mark(String action, long tick) {
    this.lastAction = String.valueOf(action);
    if ("open".equalsIgnoreCase(action)) {
      this.openedAtTick = tick;
      this.open = true;
    }
    if ("close".equalsIgnoreCase(action)) {
      this.open = false;
    }
  }
}
