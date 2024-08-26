package cpt.cheesbeard.axonsecuritycontextdemo.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cpt.cheesbeard.axonsecuritycontextdemo.command.ServerContextDelegatingCommandBusTest.TestConfig;
import cpt.cheesbeard.axonsecuritycontextdemo.message.command.CreateStuffCommand;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.axonframework.axonserver.connector.AxonServerConfiguration;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.axonframework.axonserver.connector.TargetContextResolver;
import org.axonframework.axonserver.connector.command.AxonServerCommandBus;
import org.axonframework.axonserver.connector.command.CommandLoadFactorProvider;
import org.axonframework.axonserver.connector.command.CommandPriorityCalculator;
import org.axonframework.axonserver.connector.util.ExecutorServiceBuilder;
import org.axonframework.commandhandling.AsynchronousCommandBus;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandBusSpanFactory;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.DuplicateCommandHandlerResolver;
import org.axonframework.commandhandling.distributed.RoutingStrategy;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.Configuration;
import org.axonframework.messaging.interceptors.CorrelationDataInterceptor;
import org.axonframework.serialization.Serializer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Profile("axonserver")
@SpringBootTest(classes = TestConfig.class)
class ServerContextDelegatingCommandBusTest {

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

    @Bean("localSegment")
    // changed from SimpleCommandBus to AsynchronousCommandBus, so I can add the DelegatingSecurityContextExecutorService
    public AsynchronousCommandBus commandBus(TransactionManager txManager, Configuration axonConfiguration,
        DuplicateCommandHandlerResolver duplicateCommandHandlerResolver) {
      AsynchronousCommandBus commandBus =
          AsynchronousCommandBus.builder()
              .transactionManager(txManager)
              .duplicateCommandHandlerResolver(duplicateCommandHandlerResolver)
              .spanFactory(axonConfiguration.getComponent(CommandBusSpanFactory.class))
              .messageMonitor(axonConfiguration.messageMonitor(CommandBus.class, "commandBus"))
              /* <Custom Section> */
              .executor(new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(10)))
              /* </Custom Section> */
              .build();
      commandBus.registerHandlerInterceptor(
          new CorrelationDataInterceptor<>(axonConfiguration.correlationDataProviders())
      );
      return commandBus;
    }

    @Bean
    @Primary
    public AxonServerCommandBus axonServerCommandBus(AxonServerConnectionManager axonServerConnectionManager,
        AxonServerConfiguration axonServerConfiguration,
        @Qualifier("localSegment") CommandBus localSegment,
        @Qualifier("messageSerializer") Serializer messageSerializer,
        RoutingStrategy routingStrategy,
        CommandPriorityCalculator priorityCalculator,
        CommandLoadFactorProvider loadFactorProvider,
        TargetContextResolver<? super CommandMessage<?>> targetContextResolver,
        CommandBusSpanFactory spanFactory) {
      return AxonServerCommandBus.builder()
          .axonServerConnectionManager(axonServerConnectionManager)
          .configuration(axonServerConfiguration)
          .localSegment(localSegment)
          .serializer(messageSerializer)
          .routingStrategy(routingStrategy)
          .priorityCalculator(priorityCalculator)
          .loadFactorProvider(loadFactorProvider)
          .targetContextResolver(targetContextResolver)
          .spanFactory(spanFactory)
          /* <Custom Section> */
          .executorServiceBuilder((cfg, queue) ->
              new DelegatingSecurityContextExecutorService(ExecutorServiceBuilder
                  .defaultCommandExecutorServiceBuilder().apply(cfg, queue)))
          /* </Custom Section> */
          .build();
    }


  }

}
