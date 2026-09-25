package com.mediscreen.note.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * Spring Boot's MongoDB converter, without the _class field Spring Data writes by default. A note is
 * always read back as a Note, so the field would carry nothing, and without it a note added through
 * the API has the same fields as one loaded by the seed script.
 */
@Configuration
public class MongoConfig {

    @Bean
    MappingMongoConverter mappingMongoConverter(
            MongoDatabaseFactory factory, MongoMappingContext context, MongoCustomConversions conversions) {
        var converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), context);
        converter.setCustomConversions(conversions);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null, context));
        return converter;
    }
}
