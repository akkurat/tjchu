package ch.vifian.tjchu;

import ch.taburett.tichu.cards.HandCard;
import ch.taburett.tichu.cards.Phoenix;
import ch.taburett.tichu.cards.PlayCard;
import ch.taburett.tichu.game.Played;
import ch.taburett.tichu.game.Player;
import ch.taburett.tichu.game.protocol.*;
import ch.taburett.tichu.patterns.LegalType;
import ch.taburett.tichu.patterns.Single;
import ch.taburett.tichu.patterns.TichuPattern;
import lombok.Data;
import lombok.SneakyThrows;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import static ch.taburett.tichu.cards.CardUtilsKt.pattern;
import static ch.taburett.tichu.cards.CardsKt.DRG;
import static ch.taburett.tichu.cards.CardsKt.PHX;

@Data
public class StupidUserPlayer implements UserPlayer {
    public final Player player;
    public final String name;
    public final Consumer<PlayerMessage> listener;

    @SneakyThrows
    @Override
    public void receiveServerMessage(MessageWrapper payload) {
        Thread.sleep(50);
        switch (payload.message) {
            case AckGameStage ack -> {
                switch (ack.getStage()) {
                    case EIGHT_CARDS -> listener.accept(new Ack.BigTichu());
                    case PRE_SCHUPF -> listener.accept(new Ack.TichuBeforeSchupf());
                    case SCHUPF -> {
                        var cards = ack.getCards();
                        listener.accept(new Schupf(cards.get(0), cards.get(1), cards.get(2)));
                    }
                    case POST_SCHUPF -> listener.accept(new Ack.TichuBeforePlay());
                }
            }
            case Schupf schupf -> listener.accept(new Ack.SchupfcardReceived());
            case MakeYourMove mm -> {
                if (mm.getStage() == Stage.GIFT_DRAGON) {
                    listener.accept(new GiftDragon(GiftDragon.ReLi.LI));
                } else {
                    var table = mm.getTable();
                    List<HandCard> handcards = mm.getHandcards();
                    if (!table.isEmpty()) {
                        Played toBeat = table.toBeat();
                        var pat = pattern(toBeat.getCards());
                        var all = new HashSet<>(pat.getType().patterns(handcards));
                        if (pat instanceof Single si) {
                            if (handcards.contains(DRG)) {
                                all.add(new Single(DRG));
                            }
                            if (handcards.contains(PHX)) {
                                all.add(new Single(PHX.asPlayCard(si.getCard().getValue() + 1)));
                            }
                        }
                        if (all.isEmpty()) {
                            listener.accept(new Move(List.of()));
                        } else {
                            var mypat = all.stream()
                                    .filter(p -> p.beats(pat).getType() == LegalType.OK)
                                    .min(Comparator.comparingInt(TichuPattern::rank));
                            if (mypat.isPresent()) {
                                var prPat = mypat.get();
                                if (
                                        toBeat.getPlayer().getPlayerGroup() != player.getPlayerGroup() ||
                                                pat.rank() < 10
                                ) {
                                    listener.accept(new Move(mypat.get().getCards()));
                                } else {
                                    listener.accept(new Move(List.of()));
                                }
                            } else {
                                listener.accept(new Move(List.of()));
                            }
                        }

                        // pattern
                    } else {
                        // play smallest card
                        var ocard = handcards.stream()
                                .min(Comparator.comparingDouble(HandCard::getSort))
                                .orElse(null);

                        List<PlayCard> ret = switch (ocard) {
                            case Phoenix sc -> List.of(sc.asPlayCard(1));
                            case PlayCard pc -> List.of(pc);
                            case null, default -> List.of();
                        };
                        listener.accept(new Move(ret));
                    }
                }
            }
            case null, default -> {
            }
        }
    }
}
