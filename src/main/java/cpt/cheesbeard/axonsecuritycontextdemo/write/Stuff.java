package cpt.cheesbeard.axonsecuritycontextdemo.write;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

import cpt.cheesbeard.axonsecuritycontextdemo.message.command.CreateStuffCommand;
import cpt.cheesbeard.axonsecuritycontextdemo.message.event.StuffCreatedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
public class Stuff {

  @AggregateIdentifier
  private String id;

  public Stuff() {

  }

  @CommandHandler
  public Stuff(CreateStuffCommand command) {
    apply(new StuffCreatedEvent(command.id));
  }

  @EventSourcingHandler
  public void on(StuffCreatedEvent event) {
    this.id = event.id;
  }

}
