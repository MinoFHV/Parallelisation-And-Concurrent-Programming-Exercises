package exercise3_bha9465;

import akka.actor.typed.ActorSystem;


public final class App
{

    public static void main(String[] args)
    {
        ActorSystem<Supervisor.Command> system = ActorSystem.create(Supervisor.create(10), "exercise3_bha9465");
        system.tell(new Supervisor.Started());
    }

}