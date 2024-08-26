It is possible to specify an ExecutorService for the Command/Query-Bus. Adding a `DelegatingSecurityContextExecutorService` does not bring the wanted behaviour.

<h3>CommandBus</h1>

Three tests have been added to demonstrate the problem. 

1. LocalDefaultCommandBusTest

Using the default `CommandBus` without any configuration and without axon server.<br>
As expected, `SecurityContext` is not delegated.

2. LocalContextDelegatingCommandBusTest

The `SimpleCommandBus` does not allow to set an executor, but it is possible with the `AsynchronousCommandBus`.
Using a custom `AsynchronousCommandBus` with registered `DelegatingSecurityContextExecutorService`.<br>
As expected, `SecurityContext` is delegated.


3. ServerContextDelegatingCommandBusTest

Using the bean definitions of the `spring-boot-autoconfigure` project and extend it with the `DelegatingSecurityContextExecutorService`<br>
Unexpected, the `SecurtyContext` IS NOT delegated 😢

<h3>QueryBus</h3>

There is no `AsynchronousQueryBus`, so it is not possible to configure it in a way it works locally. 
It would be nice to be able to specify an Executor that is used in  some way (on Gateway or Bus).

To Demonstrate the Problem, I added two tests.

1. LocalDefaultQueryBusTest

Using the default `QueryBus` without any configuration and without axon server.<br>
As expected, `SecurityContext` is not delegated.<br>
As mentioned, it is not possible to specify an executor in any way to make it work "locally" (without AxonServer - for Tests).

2. ServerContextDelegatingQueryBusTest

Using the bean definitions of the `spring-boot-autoconfigure` project and extend it with the `DelegatingSecurityContextExecutorService`<br>
Unexpected, the `SecurtyContext` IS NOT delegated 😢