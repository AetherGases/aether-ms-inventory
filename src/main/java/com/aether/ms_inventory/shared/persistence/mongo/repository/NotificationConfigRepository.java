package com.aether.ms_inventory.shared.persistence.mongo.repository;


import com.aether.ms_inventory.shared.persistence.mongo.documents.NotificationConfigDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationConfigRepository
    extends MongoRepository<NotificationConfigDocument, String> {

  Optional<NotificationConfigDocument> findByUserId(Integer userId);

  List<NotificationConfigDocument> findByTokensToken(String token);
}
