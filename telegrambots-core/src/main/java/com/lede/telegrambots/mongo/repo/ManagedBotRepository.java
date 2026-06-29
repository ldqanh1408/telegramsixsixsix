package com.lede.telegrambots.mongo.repo;

import com.lede.telegrambots.mongo.entity.ManagedBot;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ManagedBotRepository extends MongoRepository<ManagedBot, String> {

    Optional<ManagedBot> findByUsername(String username);

    boolean existsByUsername(String username);

    void deleteByUsername(String username);
}
