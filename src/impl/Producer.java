package it.menzani.logger.impl;

import it.menzani.logger.api.Formatter;

import java.util.*;

public final class Producer {
    private final List<Object> fragments = new ArrayList<>();
    private final Set<Formatter> formatters = new HashSet<>();

    public Producer append(Formatter formatter) {
        fragments.add(formatter);
        formatters.add(formatter);
        return this;
    }

    public Producer append(CharSequence charSequence) {
        fragments.add(charSequence);
        return this;
    }

    public Producer append(Character character) {
        fragments.add(character);
        return this;
    }

    ProducerView asView() {
        return new ProducerView(fragments, formatters);
    }
}
