package slow.kafka.consumer.util;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public class XmlDeserializer<T> implements Deserializer<T> {
    private final XmlMapper xmlMapper = new XmlMapper();
    private final Class<T> targetType;

    public XmlDeserializer(Class<T> targetType) {
        this.targetType = targetType;
    }

    public XmlDeserializer() {
        this.targetType = (Class<T>) String.class;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            return xmlMapper.readValue(data, targetType);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing XML", e);
        }
    }
}