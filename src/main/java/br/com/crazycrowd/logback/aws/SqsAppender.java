package br.com.crazycrowd.logback.aws;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class SqsAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

  @Getter
  @Setter
  private Encoder<ILoggingEvent> encoder;

  @Getter
  @Setter
  private Integer maxMessageSize = 256 * 1024;

  @Getter
  @Setter
  private String queueUrl;

  @Getter
  @Setter
  private String sqsEndpoint;

  @Getter
  private SqsClient sqsClient;

  @Override
  public void append(ILoggingEvent eventObject) {
    if (encoder == null || sqsClient == null) {
      return;
    }

    final byte[] messageBytes = encoder.encode(eventObject);

    if (messageBytes.length > maxMessageSize) {
      return;
    }

    final SendMessageRequest request =
        SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(new String(messageBytes))
            .build();

    sqsClient.sendMessage(request);
  }

  @Override
  public void start() {
    try {
      sqsClient = SqsClient.builder().endpointOverride(new URI(sqsEndpoint)).build();
      super.start();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

}
