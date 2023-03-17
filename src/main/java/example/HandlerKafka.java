package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent.KafkaEventRecord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Handler value: example.HandleKafka
public class HandlerKafka implements RequestHandler<KafkaEvent, String>{
	
  private static final Logger logger = LoggerFactory.getLogger(HandlerKafka.class);
  Gson gson = new GsonBuilder().setPrettyPrinting().create();
  
  @Override
  public String handleRequest(KafkaEvent event, Context context)
  {
    String response = new String("200 OK");
    
    // Kafka Tigger
    Map<String, List<KafkaEventRecord>> data = event.getRecords();
    
    // ActiveMQ Destination
    // Create a connection factory.
    final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("ssl://b-8c443700-bbf9-4d06-ad9f-3f9f805954fb-1.mq.ap-northeast-1.amazonaws.com:61617");

    // Pass the username and password.
    connectionFactory.setUserName("root");
    connectionFactory.setPassword("gmryu864212!");

    // Create a pooled connection factory.
    final PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
    pooledConnectionFactory.setConnectionFactory(connectionFactory);
    pooledConnectionFactory.setMaxConnections(10);
    
    MessageProducer producer = null;
    Session producerSession = null;
    Connection producerConnection = null;
    try {
	    // Establish a connection for the producer.
    	producerConnection = pooledConnectionFactory.createConnection();
	    producerConnection.start();
	    
	    // Create a session.
	    producerSession = producerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    
	    // Create a Topic named "FirstTopic".
	    final Destination producerDestination = producerSession.createTopic("FirstTopic");
	    
	    // Create a producer from the session to the queue.
	    producer = producerSession.createProducer(producerDestination);
	    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
	    
	    for(String key : data.keySet()) {
		    for(KafkaEventRecord record : data.get(key)) {
		      logger.info(key + " " + new String(Base64.getDecoder().decode(record.getValue())));
		      
		      // Create a message.
			  TextMessage producerMessage = producerSession.createTextMessage(new String(Base64.getDecoder().decode(record.getValue())));
			  
			  // Send the message.
			  producer.send(producerMessage);
		    }
	    }
    } catch (Exception e){
    	logger.error(e.toString());
    } finally {
	    try {
	    	producer.close();
			producerSession.close();
			producerConnection.close();
		} catch (JMSException e) {
			logger.error(e.toString());
		}
    }

    // Close all connections in the pool.
    pooledConnectionFactory.clear();

    return response;
  }
}