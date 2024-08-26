package cpt.cheesbeard.axonsecuritycontextdemo.query;

import static org.junit.jupiter.api.Assertions.assertNull;

import cpt.cheesbeard.axonsecuritycontextdemo.message.query.GetStuffQuery;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
class LocalDefaultQueryBusTest {

	@Autowired
	QueryGateway queryGateway;

	@Test
	void defaultCommandBus_doesntDelegateContextByDefault() throws ExecutionException, InterruptedException {
		Authentication auth = Mockito.mock(Authentication.class);
		SecurityContextHolder.getContext().setAuthentication(auth);

		Authentication delegatedAuth = queryGateway.query(new GetStuffQuery(UUID.randomUUID().toString()), String.class)
				.thenApply(result -> SecurityContextHolder.getContext().getAuthentication()).get();

		assertNull(delegatedAuth);
	}

}
