package com.lede.telegrambots.mongo.repo;

import com.lede.telegrambots.mongo.entity.GroupActivation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface GroupActivationRepository extends MongoRepository<GroupActivation, String> {

    Optional<GroupActivation> findByBotUsernameAndChatId(String botUsername, long chatId);

    List<GroupActivation> findByBotUsernameAndActiveTrue(String botUsername);

    long countByBotUsernameAndActiveTrue(String botUsername);

    boolean existsByBotUsernameAndChatIdAndActiveTrue(String botUsername, long chatId);

    void deleteByBotUsernameAndChatId(String botUsername, long chatId);

    void deleteByBotUsername(String botUsername);
}
