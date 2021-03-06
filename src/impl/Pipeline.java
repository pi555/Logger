/*
 * Copyright 2020 Francesco Menzani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.menzani.logger.impl;

import eu.menzani.logger.Cloneable;
import eu.menzani.logger.api.*;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class Pipeline implements Named, Toggleable, Cloneable<Pipeline> {
    private final String name;
    private final Set<Filter> filters = new CopyOnWriteArraySet<>();
    private volatile ProducerView producer = new Producer().append(new MessageFormatter()).asView();
    private final Set<Consumer> consumers = new CopyOnWriteArraySet<>();

    public Pipeline() {
        this(null);
    }

    public Pipeline(String name) {
        this.name = name;
    }

    @Override
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Set<Filter> getFilters() {
        return Collections.unmodifiableSet(filters);
    }

    public ProducerView getProducer() {
        return producer;
    }

    public Set<Consumer> getConsumers() {
        return Collections.unmodifiableSet(consumers);
    }

    public Pipeline setFilters(Filter... filters) {
        this.filters.clear();
        Collections.addAll(this.filters, filters);
        return this;
    }

    public Pipeline setProducer(Producer producer) {
        this.producer = producer.asView();
        return this;
    }

    public Pipeline setConsumers(Consumer... consumers) {
        this.consumers.clear();
        Collections.addAll(this.consumers, consumers);
        return this;
    }

    public Pipeline addFilter(Filter filter) {
        filters.add(filter);
        return this;
    }

    public Pipeline addConsumer(Consumer consumer) {
        consumers.add(consumer);
        return this;
    }

    public Pipeline setVerbosity(Level level) {
        addFilter(new LevelFilter(level));
        return this;
    }

    public Pipeline setDefaultVerbosity() {
        setVerbosity(StandardLevel.INFORMATION);
        return this;
    }

    public Optional<Level> getVerbosity() {
        return filters.stream()
                .filter(LevelFilter.class::isInstance)
                .map(LevelFilter.class::cast)
                .map(LevelFilter::getLevel)
                .min(new Level.Comparator());
    }

    public boolean isLoggable(Level level) {
        return getVerbosity()
                .map(level::compareTo)
                .map(verbosity -> verbosity != Level.Verbosity.GREATER)
                .orElse(true);
    }

    @Override
    public void disable() {
        addFilter(new RejectAllFilter());
    }

    @Override
    public boolean isDisabled() {
        return filters.stream()
                .anyMatch(RejectAllFilter.class::isInstance);
    }

    @Override
    public Pipeline clone() {
        Pipeline clone = new Pipeline(getName().orElse(null));
        filters.forEach(clone::addFilter);
        clone.producer = producer;
        consumers.forEach(clone::addConsumer);
        return clone;
    }

    @Override
    public String toString() {
        return getName().orElse("") + "{" +
                filters.size() + " -> " + producer + " -> " + consumers.size() + "}";
    }
}
