package aqua.client;

import aqua.common.FishModel;
import aqua.common.Properties;
import aqua.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class ClientCommunicator {
    private final Endpoint endpoint;

    public ClientCommunicator() {
        endpoint = new Endpoint();
    }

    public class ClientForwarder {
        private final InetSocketAddress broker;

        private ClientForwarder() {
            this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
        }

        public void register() {
            endpoint.send(broker, new RegisterRequest());
        }

        public void deregister(String id) {
            endpoint.send(broker, new DeregisterRequest(id));
        }

        // todo send fish directly to the right neighbor
        public void handOff(FishModel fish, InetSocketAddress target) {
            // todo old version
            endpoint.send(broker, new HandoffRequest(fish));
        }

        public void sendToken(InetSocketAddress target) {
            endpoint.send(target, new Token());
        }
    }

    public class ClientReceiver extends Thread {
        private final TankModel tankModel;

        private ClientReceiver(TankModel tankModel) {
            this.tankModel = tankModel;
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                Message message = endpoint.blockingReceive();
                Serializable payload = message.getPayload();

                if (payload instanceof RegisterResponse) {
                    tankModel.onRegistration(((RegisterResponse) payload).getId());
                } else if (payload instanceof HandoffRequest) {
                    tankModel.receiveFish(((HandoffRequest) payload).getFish());
                } else if (payload instanceof NeighborUpdate) {
                    tankModel.receiveNeighbor(((NeighborUpdate) payload).getLeftAddress(), ((NeighborUpdate) payload).getRightAddress());
                } else if (payload instanceof Token) {
                    tankModel.receiveToken();
                }
            }
            System.out.println("Receiver stopped.");
        }
    }

    public ClientForwarder newClientForwarder() {
        return new ClientForwarder();
    }

    public ClientReceiver newClientReceiver(TankModel tankModel) {
        return new ClientReceiver(tankModel);
    }

}