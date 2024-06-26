package ch.vifian.tjchu;

import ch.taburett.tichu.game.communication.Message.Points;
import ch.taburett.tichu.game.communication.Message.Rejected;
import ch.taburett.tichu.game.communication.Message.ServerMessage;
import ch.taburett.tichu.game.core.common.EPlayer;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class ProxyPlayer {

    public final EPlayer player;
    // just a string?
    @Getter
    UserPlayer userPlayerReference = null;

    Map<Class<? extends ServerMessage>, MessageWrapper> buffer = new HashMap<>();

    public ProxyPlayer(EPlayer a1) {
        this.player = a1;
    }

    public boolean unconnected() {
        return userPlayerReference == null;
    }

    public boolean connected() {
        return !unconnected();
    }

    public void connect(UserPlayer ref) {
        userPlayerReference = ref;
    }

//    @SneakyThrows
//    public void setDeck(List<HandCard> deck) {
//        this.deck = new ArrayList<>(deck);
//        playerReference.receiveServerMessage(deck);
//    }

    public void reconnected() {
        if (userPlayerReference != null) {
            for (var message : buffer.values()) {
                userPlayerReference.receiveServerMessage(message);
            }
        }
    }

    public void receiveServerMessage(MessageWrapper message) {
        if (message.message instanceof Points) {
            buffer.put(Points.class, message);
        } else if (message.message instanceof Rejected) {
            buffer.put(Rejected.class, message);
        } else {
            buffer.put(ServerMessage.class, message);
            // todo: document this logic
            // however assume a new correct message cleans out a rejection
            buffer.remove( Rejected.class);
        }
        userPlayerReference.receiveServerMessage(message);
    }
}
