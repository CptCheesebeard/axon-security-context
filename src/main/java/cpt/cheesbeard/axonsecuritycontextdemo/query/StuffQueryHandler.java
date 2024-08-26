package cpt.cheesbeard.axonsecuritycontextdemo.query;

import cpt.cheesbeard.axonsecuritycontextdemo.message.query.GetStuffQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class StuffQueryHandler {

  @QueryHandler
  public String handle(GetStuffQuery query) {
    return query.id;
  }

}
