package ro.pub.cs.systems.eim.practical2test;


import androidx.annotation.NonNull;

public class PokemonInformation {

    private final String types;

    private final String abilitys;

    public PokemonInformation(String types, String abilitys) {
        this.types = types;
        this.abilitys = abilitys;
    }

    public String getTypesPokemon() {
        return types;
    }

    public String getAbilitysPokemon() {
        return abilitys;
    }


    @NonNull
    @Override
    public String toString() {
        return "PokemonInformation {" + "type='" + types + '\'' + ", ability ='" + abilitys + '}';
    }
}

