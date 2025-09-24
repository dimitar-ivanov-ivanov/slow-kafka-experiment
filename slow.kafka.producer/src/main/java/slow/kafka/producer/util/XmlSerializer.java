package slow.kafka.producer.util;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public class XmlSerializer<T> implements Serializer<T> {
    private final XmlMapper xmlMapper = new XmlMapper();

    @Override
    public byte[] serialize(String topic, T data) {
        try {
            return xmlMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error serializing to XML", e);
        }
    }
}
