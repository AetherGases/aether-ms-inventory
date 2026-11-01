package com.aether.ms_inventory.shared.services;

import com.aether.ms_inventory.shared.persistence.mongo.documents.NotificationConfigDocument;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {
  private final ObjectProvider<FirebaseMessaging> messagingProvider;
  private final MongoTemplate mongoTemplate;

  /**
   * Envia push visivel para todos os tokens do usuario. Nunca lanca excecao.
   * Tokens invalidos sao removidos; tokens com sucesso tem lastUsedAt atualizado.
   */
  public void send(
      Integer userId,
      List<NotificationConfigDocument.Token> tokens,
      String title,
      String body
  ) {
    try {
      if (tokens == null || tokens.isEmpty()) {
        return;
      }

      FirebaseMessaging messaging = messagingProvider.getIfAvailable();

      if (messaging == null) {
        log.info("Firebase desativado; push \"{}\" para o usuario {} nao enviado", title, userId);
        return;
      }

      for (NotificationConfigDocument.Token token : tokens) {
        sendToToken(messaging, userId, token.getToken(), title, body);
      }
    } catch (RuntimeException e) {
      log.error("Falha ao enviar push para o usuario {}", userId, e);
    }
  }

  private void sendToToken(
      FirebaseMessaging messaging,
      Integer userId,
      String token,
      String title,
      String body
  ) {
    try {
      messaging.send(Message.builder()
          .setToken(token)
          .setNotification(Notification.builder().setTitle(title).setBody(body).build())
          .build());

      mongoTemplate.updateFirst(
          tokenQuery(userId, token),
          new Update().set("tokens.$.lastUsedAt", Instant.now()),
          NotificationConfigDocument.class
      );
    } catch (FirebaseMessagingException e) {
      if (isInvalidToken(e)) {
        log.info("Token invalido do usuario {} removido", userId);
        mongoTemplate.updateFirst(
            Query.query(Criteria.where("userId").is(userId)),
            new Update().pull("tokens", Query.query(Criteria.where("token").is(token))),
            NotificationConfigDocument.class
        );
      } else {
        log.error("Falha ao enviar push para o usuario {}", userId, e);
      }
    } catch (RuntimeException e) {
      log.error("Falha ao enviar push para o usuario {}", userId, e);
    }
  }

  private static Query tokenQuery(Integer userId, String token) {
    return Query.query(Criteria.where("userId").is(userId).and("tokens.token").is(token));
  }

  private static boolean isInvalidToken(FirebaseMessagingException e) {
    return e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED;
  }
}
