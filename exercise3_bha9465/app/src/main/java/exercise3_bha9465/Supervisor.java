package exercise3_bha9465;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;


public final class Supervisor
{

    public sealed interface Command permits Started, CoffeeServed {}

    public static final class Started implements Command {}
    public static final class CoffeeServed implements Command {}

    public static Behavior<Command> create(int max_coffees)
    {
        return Behaviors.setup(ctx -> new Impl(ctx, max_coffees));
    }

    private static final class Impl extends AbstractBehavior<Command>
    {

        private final int max_coffees;
        private int served = 0;

        private ActorRef<Customer.Command> customer;
        private ActorRef<Barista.Command> barista;

        public Impl(ActorContext<Command> ctx, int max_coffees)
        {

            super(ctx);
            this.max_coffees = max_coffees;

            Behavior<Barista.Command> barista_behavior = Behaviors
                .supervise(Barista.create())
                .onFailure(IllegalStateException.class, SupervisorStrategy.restart());

            barista = ctx.spawn(barista_behavior, "barista");
            customer = ctx.spawn(Customer.create(barista, ctx.getSelf()), "customer");

            getContext().getLog().info("Coffee shop open. Target: {} coffees.", max_coffees);

        }

        @Override
        public Receive<Command> createReceive()
        {

            return newReceiveBuilder()
                .onMessage(Started.class, m -> on_started())
                .onMessage(CoffeeServed.class, m -> on_coffee_served())
                .build();

        }

        private Behavior<Command> on_started()
        {

            getContext().getLog().info("Starting service ...");
            customer.tell(new Customer.BeginOrdering());

            return this;

        }

        private Behavior<Command> on_coffee_served()
        {

            served += 1;

            getContext().getLog().info("Coffee served ({}/{}).", served, max_coffees);
            
            if (served >= max_coffees)
            {
                getContext().getLog().info("Closing shop ...");
                getContext().getSystem().terminate();
            }

            return this;

        }

    }
    
}
