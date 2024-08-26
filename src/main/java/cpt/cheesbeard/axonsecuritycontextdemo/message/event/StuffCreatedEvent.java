package cpt.cheesbeard.axonsecuritycontextdemo.message.event;

public class StuffCreatedEvent {
  public String id;

  public StuffCreatedEvent(String id) {
    this.id = id;
  }
}
