package br.com.ofisy.ms_ofisy_billing.config;

import io.awspring.cloud.sns.core.CachingTopicArnResolver;
import io.awspring.cloud.sns.core.TopicArnResolver;
import io.awspring.cloud.sns.core.TopicsListingTopicArnResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class SnsConfig {

    @Bean
    public TopicArnResolver topicArnResolver(SnsClient snsClient) {
        return new CachingTopicArnResolver(new TopicsListingTopicArnResolver(snsClient));
    }
}
