package com.iwfc.repository;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.model.Identifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generic Repository providing standard CRUD operations and query methods.
 * Demonstrates the use of Generics and Collections.
 *
 * @param <T> the entity type extending Identifiable
 */
public class Repository<T extends Identifiable> {

    private final Map<String, T> items = new HashMap<>();

    public Optional<T> findById(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public List<T> findAll() {
        return new ArrayList<>(items.values());
    }

    public void save(T item) throws DuplicateDataException {
        if (items.containsKey(item.getId())) {
            throw new DuplicateDataException("An item with ID " + item.getId() + " already exists.");
        }
        items.put(item.getId(), item);
    }

    public void update(T item) {
        // Simple update: overwrites the existing item.
        items.put(item.getId(), item);
    }

    public void delete(String id) {
        items.remove(id);
    }

    public List<T> findBy(Predicate<T> criteria) {
        return items.values().stream()
            .filter(criteria)
            .collect(Collectors.toList());
    }

    public <R> List<R> mapTo(Function<T, R> mapper) {
        return items.values().stream()
            .map(mapper)
            .collect(Collectors.toList());
    }
}
