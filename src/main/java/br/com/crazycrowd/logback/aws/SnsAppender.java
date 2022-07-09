package br.com.crazycrowd.logback.aws;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class SnsAppender extends AppenderBase<ILoggingEvent> {

  @Getter
  @Setter
  private Encoder<ILoggingEvent> encoder;

  @Getter
  @Setter
  private Integer maxMessageSize = 256 * 1024;

  @Getter
  @Setter
  private String snsEndpoint;

  @Getter
  @Setter
  private String topicArn;

  @Getter
  private SnsClient snsClient;

  @Override
  public void append(final ILoggingEvent eventObject) {
    if (encoder == null || snsClient == null) {
      return;
    }

    final byte[] notificationBytes = encoder.encode(eventObject);

    if (notificationBytes.length > maxMessageSize) {
      return;
    }

    final PublishRequest request =
        PublishRequest.builder()
            .message(new String(notificationBytes))
            .topicArn(topicArn)
            .build();

    snsClient.publish(request);
  }

  @Override
  public void start() {
    try {
      snsClient = SnsClient.builder().endpointOverride(new URI(snsEndpoint)).build();
      super.start();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

}
