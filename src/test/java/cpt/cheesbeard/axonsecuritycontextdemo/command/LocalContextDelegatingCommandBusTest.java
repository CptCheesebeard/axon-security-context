package cpt.cheesbeard.axonsecuritycontextdemo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cpt.cheesbeard.axonsecuritycontextdemo.command.LocalContextDelegatingCommandBusTest.TestConfig;
import cpt.cheesbeard.axonsecuritycontextdemo.message.command.CreateStuffCommand;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.axonframework.commandhandling.AsynchronousCommandBus;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(classes = TestConfig.class)
class LocalContextDelegatingCommandBusTest {

  @Autowired
  CommandGateway commandGateway;

  @Test
  void customLocalCommandBus_delegatesContext() throws ExecutionException, InterruptedException {
    Authentication auth = Mockito.mock(Authentication.class);
    SecurityContextHolder.getContext().setAuthentication(auth);

    Authentication delegatedAuth = commandGateway.send(new CreateStuffCommand(UUID.randomUUID().toString()))
        .thenApply(result -> SecurityContextHolder.getContext().getAuthentication()).get();

    assertEquals(auth, delegatedAuth);
  }

  @TestConfiguration
  public static class TestConfig {

    @Bean
    public CommandBus localContextDelegatingCommandBus() {
      return AsynchronousCommandBus.builder()
          .executor(new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(10)))
          .build();
    }

  }

}
