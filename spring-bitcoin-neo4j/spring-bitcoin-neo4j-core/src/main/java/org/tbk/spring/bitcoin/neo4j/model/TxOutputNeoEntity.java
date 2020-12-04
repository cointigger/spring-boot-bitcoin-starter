package org.tbk.spring.bitcoin.neo4j.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashMap;
import java.util.Map;


@Data
@EqualsAndHashCode(of = "id")
@Node("txo")
public class TxOutputNeoEntity {

    // id is "$tx_hash:$index"
    @Id
    private String id;

    // @Required
    private long index;

    // @Required
    private long value;

    // size in bytes
    private int size;

    @CompositeProperty(prefix = "meta")
    private Map<String, Object> meta = new HashMap<>();

    @Relationship(type = "ADDRESS", direction = Relationship.Direction.OUTGOING)
    private AddressNeoRel address;

    // @Required
    @Relationship(type = "OUT", direction = Relationship.Direction.INCOMING)
    private InNeoRel createdIn;

    @Relationship(type = "IN", direction = Relationship.Direction.OUTGOING)
    private InNeoRel spentBy;

    public boolean isUnspent() {
        return spentBy == null;
    }
}
