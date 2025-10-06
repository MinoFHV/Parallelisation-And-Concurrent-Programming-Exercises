package exercise3_bha9465;

import java.time.Duration;
import java.util.Random;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;


public final class Barista
{

    public sealed interface Command permits PrepareOrder, CompleteOrder {}

    public static final class PrepareOrder implements Command
    {

        public final int hour;
        public final Coffee coffee;
        public final ActorRef<Customer.Command> reply_to;

        public PrepareOrder(int hour, Coffee coffee, ActorRef<Customer.Command> reply_to)
        {

            this.hour = hour;
            this.coffee = coffee;
            this.reply_to = reply_to;

        }

    }

    public static final class CompleteOrder implements Command
    {
        public final PrepareOrder order;
        public CompleteOrder(PrepareOrder order) { this.order = order; }
    }

    // ----- Behavior -----
    public static Behavior<Command> create()
    {
        return Behaviors.setup(ctx -> Behaviors.withTimers(timers -> new Impl(ctx, timers)));
    }

    private static final class Impl extends AbstractBehavior<Command>
    {

        private final TimerScheduler<Command> timers;
        private final Random rnd = new Random();

        public Impl(ActorContext<Command> ctx, TimerScheduler<Command> timers)
        {
            super(ctx);
            this.timers = timers;
        }

        @Override
        public Receive<Command> createReceive()
        {

            return newReceiveBuilder()
                .onMessage(PrepareOrder.class, this::on_prepare)
                .onMessage(CompleteOrder.class, this::on_complete)
                .build();

        }

        private Behavior<Command> on_prepare(PrepareOrder msg)
        {

            if ((msg.coffee == Coffee.CAPPUCCINO) && (msg.hour > 12))
            {
                getContext().getLog().warn("Rule break: Cappuccino ordered after 12 ({}:00). Crashing ...", msg.hour);
                throw new IllegalStateException("No cappuccino after 12!");
            }

            getContext().getLog().info("Preparing {} ...", msg.coffee);

            int millis = 1000 + rnd.nextInt(2000);
            timers.startSingleTimer(new CompleteOrder(msg), Duration.ofMillis(millis));
            
            return this;

        }

        private Behavior<Command> on_complete(CompleteOrder c)
        {
            c.order.reply_to.tell(new Customer.CoffeeReady(c.order.coffee, c.order.hour));
            return this;
        }

    }
    
}