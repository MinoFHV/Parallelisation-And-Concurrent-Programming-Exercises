package exercise3_bha9465;

import java.time.Duration;
import java.util.List;
import java.util.Random;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;


public final class Customer
{

    public sealed interface Command permits BeginOrdering, PlaceOrderTick, CoffeeReady {}

    public static final class BeginOrdering implements Command {}
    public static final class PlaceOrderTick implements Command {}

    public static final class CoffeeReady implements Command
    {

        public final Coffee coffee;
        public final int hour;

        public CoffeeReady(Coffee coffee, int hour)
        {
            this.coffee = coffee;
            this.hour = hour;
        }

    }

    private static final List<Coffee> MENU = List.of(Coffee.LATTE, Coffee.CAPPUCCINO, Coffee.ESPRESSO);

    public static Behavior<Command> create(ActorRef<Barista.Command> barista, ActorRef<Supervisor.Command> supervisor)
    {
        return Behaviors.setup(ctx -> Behaviors.withTimers(timers -> new Impl(ctx, timers, barista, supervisor)));
    }

    private static final class Impl extends AbstractBehavior<Command>
    {

        private final TimerScheduler<Command> timers;
        private final ActorRef<Barista.Command> barista;
        private final ActorRef<Supervisor.Command> supervisor;
        private final Random rnd = new Random();

        public Impl(ActorContext<Command> ctx, TimerScheduler<Command> timers, ActorRef<Barista.Command> barista, ActorRef<Supervisor.Command> supervisor)
        {

            super(ctx);
            this.timers = timers;
            this.barista = barista;
            this.supervisor = supervisor;

        }

        @Override
        public Receive<Command> createReceive()
        {

            return newReceiveBuilder()
                .onMessage(BeginOrdering.class, m -> on_begin())
                .onMessage(PlaceOrderTick.class, m -> on_tick())
                .onMessage(CoffeeReady.class, this::on_coffee_ready)
                .build();
                
        }

        private Behavior<Command> on_begin()
        {
            timers.startTimerAtFixedRate(new PlaceOrderTick(), Duration.ofMillis(600));
            return this;
        }

        private Behavior<Command> on_tick()
        {

            Coffee coffee = MENU.get(rnd.nextInt(MENU.size()));
            int hour = rnd.nextInt(24);

            getContext().getLog().info("Customer orders {} at {}:00", coffee, hour);
            barista.tell(new Barista.PrepareOrder(hour, coffee, getContext().getSelf()));

            return this;

        }

        private Behavior<Command> on_coffee_ready(CoffeeReady msg)
        {

            getContext().getLog().info("Received {} - YUMMERS!", msg.coffee);
            supervisor.tell(new Supervisor.CoffeeServed());

            return this;

        }
        
    }

}