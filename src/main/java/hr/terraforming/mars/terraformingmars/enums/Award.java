package hr.terraforming.mars.terraformingmars.enums;

import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.Player;
import lombok.Getter;

import java.util.function.BiFunction;

public enum Award {
    LANDLORD("Landlord", "Most tiles in play.", (player, board) ->
            (int) board.getTiles().stream().filter(t -> player.equals(t.getOwner())).count()),

    BANKER("Banker", "Highest MC production.", (player, _) ->
            player.getProduction(ResourceType.MEGA_CREDITS)),

    SCIENTIST("Scientist", "Most science tags in play.", (player, _) ->
            player.countTags(TagType.SCIENCE)),

    THERMALIST("Thermalist", "Most heat resource cubes.", (player, _) ->
            player.resourceProperty(ResourceType.HEAT).get()),

    MINER("Miner", "Most steel and titanium resource cubes.", (player, _) ->
            player.resourceProperty(ResourceType.STEEL).get() + player.resourceProperty(ResourceType.TITANIUM).get());

    @Getter
    private final String name;
    @Getter
    private final String description;
    private final BiFunction<Player, GameBoard, Integer> evaluator;

    Award(String name, String description, BiFunction<Player, GameBoard, Integer> evaluator) {
        this.name = name;
        this.description = description;
        this.evaluator = evaluator;
    }

    public int evaluateScore(Player player, GameBoard board) {
        return evaluator.apply(player, board);
    }
}