package com.aether.ms_inventory.notification;

import com.aether.ms_inventory.shared.persistence.mongo.documents.NotificationConfigDocument;
import com.aether.ms_inventory.shared.services.PushNotificationService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PushNotificationService Tests")
class PushNotificationServiceTests {

  @Mock private ObjectProvider<FirebaseMessaging> messagingProvider;
  @Mock private MongoTemplate mongoTemplate;
  @Mock private FirebaseMessaging messaging;

  @InjectMocks private PushNotificationService service;

  private final List<NotificationConfigDocument.Token> tokens = List.of(
      new NotificationConfigDocument.Token("t1", null, null),
      new NotificationConfigDocument.Token("t2", null, null)
  );

  @Test
  @DisplayName("Should send one push per token and update lastUsedAt")
  void sendsToEveryToken() throws Exception {
    when(messagingProvider.getIfAvailable()).thenReturn(messaging);

    service.send(7, tokens, "Titulo", "Corpo");

    verify(messaging, times(2)).send(any(Message.class));
    verify(mongoTemplate, times(2)).updateFirst(
        any(Query.class), any(Update.class), eq(NotificationConfigDocument.class));
  }

  @Test
  @DisplayName("Should do nothing when Firebase is disabled")
  void skipsWhenFirebaseDisabled() {
    when(messagingProvider.getIfAvailable()).thenReturn(null);

    service.send(7, tokens, "Titulo", "Corpo");

    verifyNoInteractions(messaging, mongoTemplate);
  }

  @Test
  @DisplayName("Should do nothing when the user has no tokens")
  void skipsWithoutTokens() {
    service.send(7, List.of(), "Titulo", "Corpo");
    service.send(7, null, "Titulo", "Corpo");

    verifyNoInteractions(messagingProvider, messaging, mongoTemplate);
  }

  @Test
  @DisplayName("Should remove an unregistered token and keep sending to the others")
  void removesUnregisteredToken() throws Exception {
    when(messagingProvider.getIfAvailable()).thenReturn(messaging);

    FirebaseMessagingException unregistered = mock(FirebaseMessagingException.class);
    when(unregistered.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
    when(messaging.send(any(Message.class))).thenThrow(unregistered).thenReturn("id");

    service.send(7, tokens, "Titulo", "Corpo");

    verify(messaging, times(2)).send(any(Message.class));
    // 1 pull do token invalido + 1 atualizacao de lastUsedAt do token valido
    verify(mongoTemplate, times(2)).updateFirst(
        any(Query.class), any(Update.class), eq(NotificationConfigDocument.class));
  }

  @Test
  @DisplayName("Should keep the token and not throw on other Firebase errors")
  void keepsTokenOnOtherErrors() throws Exception {
    when(messagingProvider.getIfAvailable()).thenReturn(messaging);

    FirebaseMessagingException unavailable = mock(FirebaseMessagingException.class);
    when(unavailable.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);
    when(messaging.send(any(Message.class))).thenThrow(unavailable);

    service.send(7, tokens, "Titulo", "Corpo");

    verify(messaging, times(2)).send(any(Message.class));
    verify(mongoTemplate, never()).updateFirst(
        any(Query.class), any(Update.class), eq(NotificationConfigDocument.class));
  }
}
