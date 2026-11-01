package com.aether.ms_inventory.shared.persistence.mongo.repository;

import com.aether.ms_inventory.shared.persistence.mongo.documents.NotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository
    extends MongoRepository<NotificationDocument, String> {
}
