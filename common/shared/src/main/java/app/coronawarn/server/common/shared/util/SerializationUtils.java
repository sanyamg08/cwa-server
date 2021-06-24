package app.coronawarn.server.common.shared.util;


import app.coronawarn.server.common.shared.exception.DefaultValueSetsMissingException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.asm.TypeReference;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ObjectUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

public final class SerializationUtils {

  private static final Logger logger = LoggerFactory.getLogger(SerializationUtils.class);

  /**
   * Deserialize json string into an object of type T. The type must also be provided to the underlying Jackson library
   * in the form of a JavaType supplied by the function parameter.
   *
   * @param jsonString           value from configuration file
   * @param typeProviderFunction type deserialization function provider
   * @param <T>                  generic type
   * @return deserialized json string
   */
  public static <T> T deserializeJson(final String jsonString,
      final Function<TypeFactory, JavaType> typeProviderFunction) {
    final ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature()).readValue(jsonString,
          typeProviderFunction.apply(mapper.getTypeFactory()));
    } catch (final JsonProcessingException e) {
      throw new IllegalStateException("Json configuration could not be deserialized", e);
    }
  }

  /**
   * Parse json from stream instead from string.
   *
   * @param jsonStream           stream to read json from
   * @param typeProviderFunction type deserialization function provider
   * @return deserialized json as pojo
   * @throws IOException coming from {@link ObjectMapper#readValue(InputStream, JavaType)}.
   */
  public static <T> T deserializeJson(final InputStream jsonStream,
      final Function<TypeFactory, JavaType> typeProviderFunction) throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    return mapper.enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature())
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .readValue(jsonStream, typeProviderFunction.apply(mapper.getTypeFactory()));
  }

  /**
   * Stringify json object T. T must be a valid Jackson object in order for properties to be correctly parsed into the
   * string. Null values will be omitted.
   *
   * @param object Jackson object.
   * @param <T>    valid object with JsonProperty notations.
   * @return String encoded JSON.
   */
  public static <T> String stringifyObject(final T object) {
    final ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(object);
    } catch (final JsonProcessingException e) {
      throw new IllegalStateException("Object could not be converted into JSON", e);
    }
  }

  public static <T> T deserializeJsonToSimpleType(InputStream jsonStream, Class<T> rawType) throws IOException {
    return deserializeJson(jsonStream,
        typeFactory -> typeFactory.constructSimpleType(rawType, new JavaType[0]));
  }

  public static <T> T readConfiguredJsonOrDefault(ResourceLoader resourceLoader,
      String path,
      String defaultPath,
      Class<T> rawType)
      throws DefaultValueSetsMissingException {
    if (!ObjectUtils.isEmpty(path)) {
      try (InputStream jsonStream = resourceLoader.getResource(path).getInputStream()) {
        logger.debug("Loading JSON from {}.", path);
        return deserializeJsonToSimpleType(jsonStream, rawType);
      } catch (IOException e) {
        logger.error("Error reading {} from json {}.", rawType.getSimpleName(), path, e);
      }
    }
    try (InputStream jsonStream = resourceLoader.getResource(defaultPath).getInputStream()) {
      // fallback to default
      logger.debug("JSON to load was empty or invalid, falling back to loading from {}.", defaultPath);
      return deserializeJsonToSimpleType(jsonStream, rawType);
    } catch (IOException e) {
      logger.error("We could not load the default {}. This shouldn't happen!", defaultPath, e);
      throw new DefaultValueSetsMissingException("Default valuesets is missing from the path " + defaultPath
          + ". This shouldn't happen!", e);
    }
  }

  private SerializationUtils() {
  }

}
