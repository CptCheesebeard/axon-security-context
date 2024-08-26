package cpt.cheesbeard.axonsecuritycontextdemo.command;

import static org.junit.jupiter.api.Assertions.assertNull;

import cpt.cheesbeard.axonsecuritycontextdemo.message.command.CreateStuffCommand;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
class LocalDefaultCommandBusTest {

	@Autowired
	CommandGateway commandGateway;

	@Test
	void defaultCommandBus_doesntDelegateContextByDefault() throws ExecutionException, InterruptedException {
		Authentication auth = Mockito.mock(Authentication.class);
		SecurityContextHolder.getContext().setAuthentication(auth);

		Authentication delegatedAuth = commandGateway.send(new CreateStuffCommand(UUID.randomUUID().toString()))
				.thenApply(result -> SecurityContextHolder.getContext().getAuthentication()).get();

		assertNull(delegatedAuth);
	}
}
