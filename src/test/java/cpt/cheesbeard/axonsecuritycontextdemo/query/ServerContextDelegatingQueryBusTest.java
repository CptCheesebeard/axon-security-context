package cpt.cheesbeard.axonsecuritycontextdemo.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cpt.cheesbeard.axonsecuritycontextdemo.message.query.GetStuffQuery;
import cpt.cheesbeard.axonsecuritycontextdemo.query.ServerContextDelegatingQueryBusTest.TestConfig;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.axonframework.axonserver.connector.AxonServerConfiguration;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.axonframework.axonserver.connector.TargetContextResolver;
import org.axonframework.axonserver.connector.query.AxonServerQueryBus;
import org.axonframework.axonserver.connector.query.QueryPriorityCalculator;
import org.axonframework.axonserver.connector.util.ExecutorServiceBuilder;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.messaging.interceptors.CorrelationDataInterceptor;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryBusSpanFactory;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryInvocationErrorHandler;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.axonframework.queryhandling.SimpleQueryBus;
import org.axonframework.serialization.Serializer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Profile("axonserver")
@SpringBootTest(classes = TestConfig.class)
class ServerContextDelegatingQueryBusTest {

  @Autowired
  QueryGateway queryGateway;

  @Test
  void customLocalCommandBus_delegatesContext() throws ExecutionException, InterruptedException {
    Authentication auth = Mockito.mock(Authentication.class);
    SecurityContextHolder.getContext().setAuthentication(auth);

    Authentication delegatedAuth = queryGateway.query(new GetStuffQuery(UUID.randomUUID().toString()), String.class)
        .thenApply(result -> SecurityContextHolder.getContext().getAuthentication()).get();

    assertEquals(auth, delegatedAuth);
  }

  @TestConfiguration
  public static class TestConfig {

    @Bean
    public AxonServerQueryBus queryBus(AxonServerConnectionManager axonServerConnectionManager,
        AxonServerConfiguration axonServerConfiguration,
        org.axonframework.config.Configuration axonConfiguration,
        TransactionManager txManager,
        @Qualifier("messageSerializer") Serializer messageSerializer,
        Serializer genericSerializer,
        QueryPriorityCalculator priorityCalculator,
        QueryInvocationErrorHandler queryInvocationErrorHandler,
        TargetContextResolver<? super QueryMessage<?, ?>> targetContextResolver) {

      // No possibility to set executor
      SimpleQueryBus simpleQueryBus =
          SimpleQueryBus.builder()
              .messageMonitor(axonConfiguration.messageMonitor(QueryBus.class, "queryBus"))
              .transactionManager(txManager)
              .queryUpdateEmitter(axonConfiguration.getComponent(QueryUpdateEmitter.class))
              .errorHandler(queryInvocationErrorHandler)
              .spanFactory(axonConfiguration.getComponent(QueryBusSpanFactory.class))
              .build();
      simpleQueryBus.registerHandlerInterceptor(
          new CorrelationDataInterceptor<>(axonConfiguration.correlationDataProviders())
      );

      return AxonServerQueryBus.builder()
          .axonServerConnectionManager(axonServerConnectionManager)
          .configuration(axonServerConfiguration)
          .localSegment(simpleQueryBus)
          .updateEmitter(simpleQueryBus.queryUpdateEmitter())
          .messageSerializer(messageSerializer)
          .genericSerializer(genericSerializer)
          .priorityCalculator(priorityCalculator)
          .targetContextResolver(targetContextResolver)
          .spanFactory(axonConfiguration.getComponent(QueryBusSpanFactory.class))
          /* <Custom Section> */
          .executorServiceBuilder((cfg, queue) ->
              new DelegatingSecurityContextExecutorService(ExecutorServiceBuilder
                  .defaultCommandExecutorServiceBuilder().apply(cfg, queue)))
          /* </Custom Section> */
          .build();
    }


  }

}
