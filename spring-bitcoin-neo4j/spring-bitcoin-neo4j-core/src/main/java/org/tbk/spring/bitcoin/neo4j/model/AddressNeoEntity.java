package org.tbk.spring.bitcoin.neo4j.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
@EqualsAndHashCode(of = "address")
@Node("addr")
public class AddressNeoEntity {

    @Id
    private String address;

    @Relationship(type = "ADDRESS", direction = Relationship.Direction.INCOMING)
    private List<OutNeoRel> outputs;

    @CompositeProperty(prefix = "meta")
    private Map<String, Object> meta = new HashMap<>();
}
